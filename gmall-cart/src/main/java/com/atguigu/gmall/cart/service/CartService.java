package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVoS;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:item:";

    private static final String KEY_PRICE = "cart:price:";

    public void addCart(Cart cart) {

        String key = KEY_PREFIX;
        //获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if(userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        //1.获取购物车信息
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        //2.判断购物车中是否有该商品
        if(hashOps.hasKey(skuId)){
            //3.有,更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount() + count);
        }else {
            //4.无，新增
            cart.setCheck(true);
            //查询sku相关信息
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if(skuInfoEntity == null){
                return;
            }
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setImage(skuInfoEntity.getSkuDefaultImg());
            cart.setSkuTitle(skuInfoEntity.getSkuTitle());

            //查询库存相关信息
            Resp<List<WareSkuEntity>> wareResp = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> skuEntities = wareResp.getData();
            if(CollectionUtils.isEmpty(skuEntities)){
                cart.setStore(skuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
            //查询销售属性
            Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClient.querySaleAttrValueSkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> saleAttrValueEntities = saleAttrResp.getData();
            cart.setSaleAttrs(saleAttrValueEntities);
            
            //查询营销信息
            Resp<List<SaleVoS>> saleResp = smsClient.queryItemSaleVoBySkuId(cart.getSkuId());
            List<SaleVoS> saleVos = saleResp.getData();
            cart.setSales(saleVos);

            redisTemplate.opsForValue().set(KEY_PRICE + skuId,skuInfoEntity.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    public List<Cart> queryCarts() {

        //获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        Long userId = userInfo.getUserId();

        // 先查询未登录的购物车
        userKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> UserKeyHashOps = redisTemplate.boundHashOps(userKey);
        List<Object> values = UserKeyHashOps.values();
        List<Cart> userKeyCarts = null;
        if(!CollectionUtils.isEmpty(values)){
            userKeyCarts = values.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //查询当前价格
                String currentPrice = redisTemplate.opsForValue().get(KEY_PRICE + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));

                return cart;
            }).collect(Collectors.toList());
        }
        //判断是否登录，未登录直接返回
        if(userId == null){
            return userKeyCarts;
        }
        //登录了合并未登录的购物车到登录的购物车
        String userIdKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> userIdHashOps = redisTemplate.boundHashOps(userIdKey);
        if(!CollectionUtils.isEmpty(userKeyCarts)){
            userKeyCarts.forEach(cart -> {
                if(userIdHashOps.hasKey(cart.getSkuId().toString())){//如果登陆状态下有该记录就更新数量
                    String cartJson = userIdHashOps.get(cart.getSkuId().toString()).toString();
                    Integer count = cart.getCount();
                    JSON.parseObject(cartJson,Cart.class);
                    cart.setCount(cart.getCount() + count);
                }//如果登录状态下没有该记录，直接添加
                userIdHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
            //删除未登录的购物车
            redisTemplate.delete(userKey);
        }
        //查询展示
        List<Object> userIdCartJsons = userIdHashOps.values();
        if(!CollectionUtils.isEmpty(userIdCartJsons)){
            return userIdCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //查询当前价格
                //String currentPrice = redisTemplate.opsForValue().get(KEY_PRICE + cart.getSkuId());
                String currentPrice = this.redisTemplate.opsForValue().get(KEY_PRICE + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    public void updateNum(Cart cart) {

        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if(userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        //获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //判断购物车中有没有这个数据
        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Integer count = cart.getCount();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    public void check(Cart cart) {
        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if(userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        //获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //判断购物车中有没有这个数据
        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Boolean check = cart.getCheck();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    public void delete(Long skuId) {
        //获取登录状态信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if(userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        //获取内层的map
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //判断购物车中有没有这个数据
        if(hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
        }
    }

    public List<Cart> queryCheckedCarts(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);

        //获取所有的购物车记录
        List<Object> values = hashOps.values();
        if(!CollectionUtils.isEmpty(values)){
            return values.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class)).filter(cart -> cart.getCheck()).collect(Collectors.toList());
        }
        return null;
    }
}
