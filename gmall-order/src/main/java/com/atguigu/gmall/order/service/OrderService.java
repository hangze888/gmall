package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVoS;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String TOKEN_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {

        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        //获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //获取用户的地址信息
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> listResp = umsClient.queryAddressByUserId(userInfo.getUserId());
            List<MemberReceiveAddressEntity> addressEntities = listResp.getData();
            orderConfirmVo.setAddress(addressEntities);
        }, threadPoolExecutor);

        //获取订单的详情列表
        CompletableFuture<Void> itemsFuture = CompletableFuture.supplyAsync(() -> {
            return cartClient.queryCheckedCarts(userInfo.getUserId());
        }).thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItems = carts.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                Integer count = cart.getCount();
                OrderItemVo orderItemVo = new OrderItemVo();

                orderItemVo.setCount(count);
                orderItemVo.setSkuId(skuId);

                //查询sku相关信息
                CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                    Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                        orderItemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
                    }
                }, threadPoolExecutor);

                //查询商品的库存信息
                CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.queryWareSkuBySkuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    }
                }, threadPoolExecutor);

                //查询销售属性
                CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClient.querySaleAttrValueSkuId(skuId);
                    List<SkuSaleAttrValueEntity> attrValueEntities = saleAttrResp.getData();
                    if (!CollectionUtils.isEmpty(attrValueEntities)) {
                        orderItemVo.setSaleAttrs(attrValueEntities);
                    }
                }, threadPoolExecutor);

                //查询营销信息
                CompletableFuture<Void> saleFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SaleVoS>> itemSaleResp = smsClient.queryItemSaleVoBySkuId(skuId);
                    List<SaleVoS> saleRespDatas = itemSaleResp.getData();
                    if (!CollectionUtils.isEmpty(saleRespDatas)) {
                        orderItemVo.setSales(saleRespDatas);
                    }
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuFuture, wareFuture, saleAttrFuture, saleFuture).join();
                return orderItemVo;

            }).collect(Collectors.toList());
            orderConfirmVo.setOrderItems(orderItems);
        }, threadPoolExecutor);


        //获取用户的积分信息
        CompletableFuture<Void> boundsFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = umsClient.queryMemberById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity != null) {
                orderConfirmVo.setBounds(memberEntity.getIntegration());
            }
        }, threadPoolExecutor);
        
        //防止重复提交的唯一标识
        // 分布式id生成器（mybatis-plus提供）
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken);//浏览器一份
            redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture,itemsFuture,boundsFuture,tokenFuture).join();

        return orderConfirmVo;
    }

    public OrderEntity submit(OrderSubmitVo orderSubmitVo) {

        // 1.校验是否重复提交（是：提示， 否：跳转到支付页面，创建订单）
        //判断redis中有没有，有说明第一次提交，放行并删除redis中orderToken
        String orderToken = orderSubmitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = (Long)redisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList(TOKEN_PREFIX + orderSubmitVo.getOrderToken()), orderToken);
        if(flag == 0){
            throw new OrderException("请不要重复提交订单");
        }

        // 2.验价(总价格是否发生了变化)
        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();//页面提交的总价格
        //获取数据库的实时价格
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("请勾选要购买的商品");
        }
        BigDecimal currentTotalPrice = items.stream().map(orderItemVo -> {
            Long skuId = orderItemVo.getSkuId();
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVo.getCount())); //获取sku的实时价格 * count
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        //比较价格是否一致
        if(totalPrice.compareTo(currentTotalPrice) != 0){
            throw new OrderException("页面已过期，请刷新后再试!");
        }

        // 3.验库并锁定库存（具备原子性，支付成功后，才是真正减库存）
        List<SkuLockVo> skuLockVos = items.stream().map(orderItemVo -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(orderItemVo.getSkuId());
            skuLockVo.setCount(orderItemVo.getCount());
            skuLockVo.setOrderToken(orderSubmitVo.getOrderToken());
            return skuLockVo;
        }).collect(Collectors.toList());

        Resp<List<SkuLockVo>> skuLockResp = wmsClient.checkAndLock(skuLockVos);
        List<SkuLockVo> skuLockVoList = skuLockResp.getData();
        if(!CollectionUtils.isEmpty(skuLockVoList)){
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }
        // 异常 ： 后续订单无法创建，定时释放库存

        // 4.新增订单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        OrderEntity orderEntity = null;
        try {
            Resp<OrderEntity> orderEntityResp = omsClient.saveOrder(orderSubmitVo, userInfo.getUserId());
            orderEntity = orderEntityResp.getData();
        }catch (Exception e){
            e.printStackTrace();
            //订单创建异常应该立马释放 ：feign（会阻塞业务） 消息队列（异步）
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.unlock",orderSubmitVo.getOrderToken());

            throw new OrderException("订单保存失败，服务错误");
        }

        // 5.删除购物车中相应的记录
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId",userInfo.getUserId());
            List<Long> skuIds = items.stream().map(orderItemVo -> orderItemVo.getSkuId()).collect(Collectors.toList());
            map.put("skuIds",JSON.toJSONString(skuIds));
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","cart.delete",map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
        return orderEntity;
    }
}
