package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private AmqpTemplate amqpTemplate;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public OrderEntity saveOrder(OrderSubmitVo orderSubmitVo, Long userId) {
        //新增主表（订单表）
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSubmitVo.getOrderToken());//订单编号
        orderEntity.setTotalAmount(orderSubmitVo.getTotalPrice());
        orderEntity.setPayType(orderSubmitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setDeliveryCompany(orderSubmitVo.getDeliveryCompany());
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setStatus(0);
        orderEntity.setDeleteStatus(0);
//      orderEntity.setGrowth(); // 通过购买商品的积分优惠信息设置
        orderEntity.setMemberId(userId);

        MemberReceiveAddressEntity address = orderSubmitVo.getAddress();
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
//        orderEntity.setMemberUsername(); // 根据查询用户名

        boolean flag = this.save(orderEntity);
        //新增子表（订单详情表）
        if(flag){
            List<OrderItemVo> items = orderSubmitVo.getItems();
            items.forEach(orderItemVo -> {
                OrderItemEntity orderItemEntity = new OrderItemEntity();
                orderItemEntity.setOrderSn(orderSubmitVo.getOrderToken());
                orderItemEntity.setOrderId(orderEntity.getId());
                //先查询sku的信息
                Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(orderItemVo.getSkuId());
                SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                if(skuInfoEntity != null){
                    orderItemEntity.setSkuId(orderItemVo.getSkuId());
                    orderItemEntity.setSkuQuantity(orderItemVo.getCount());
                    orderItemEntity.setSkuPic(orderItemVo.getImage());
                    orderItemEntity.setSkuName(orderItemVo.getSkuTitle());
                    orderItemEntity.setSkuAttrsVals(JSON.toJSONString(orderItemVo.getSaleAttrs()));
                    orderItemEntity.setSkuPrice(skuInfoEntity.getPrice());

                    //根据spuId查询，spu设置spu信息
                    Long spuId = skuInfoEntity.getSpuId();
                    Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(spuId);
                    SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                    if(spuInfoEntity != null){
                        orderItemEntity.setSpuId(spuId);
                        orderItemEntity.setSpuName(spuInfoEntity.getSpuName());
                        orderItemEntity.setSpuBrand(spuInfoEntity.getBrandId().toString());
                        orderItemEntity.setCategoryId(spuInfoEntity.getCatalogId());
                    }
                    //查询优惠信息，设置优惠信息
                }
                orderItemDao.insert(orderItemEntity);
            });
        }

        //订单创建完成之后，定时关单
        amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.ttl",orderSubmitVo.getOrderToken());
        return orderEntity;
    }

}