package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockListener {
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "wms:stock:";

    @Autowired
    private WareSkuDao wareSkuDao;
    
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-UNLOCK-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock","wms.dead"}
    ))
    public void unlock(String orderToken){
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);

        if(StringUtils.isEmpty(json)){
            return ;
        }

        // 反序列化锁定库存信息
        List<SkuLockVo> skuLockVoList = JSON.parseArray(json, SkuLockVo.class);
        skuLockVoList.forEach(skuLockVo -> {
            wareSkuDao.unLock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            redisTemplate.delete(KEY_PREFIX + orderToken);
        });
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-MINUS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minus(String orderToken){
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);

        if(StringUtils.isEmpty(json)){
            return ;
        }

        // 反序列化锁定库存信息
        List<SkuLockVo> skuLockVoList = JSON.parseArray(json, SkuLockVo.class);
        skuLockVoList.forEach(skuLockVo -> {
            wareSkuDao.minus(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            redisTemplate.delete(KEY_PREFIX + orderToken);
        });
    }

}
