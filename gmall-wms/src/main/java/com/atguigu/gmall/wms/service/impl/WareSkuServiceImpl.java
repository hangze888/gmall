package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import net.bytebuddy.asm.Advice;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String KEY_PREFIX = "wms:stock:";

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> skuLockVos) {

        //判断传递的数据是否为空
        if(CollectionUtils.isEmpty(skuLockVos)){
            return null;
        }

        //遍历清单集合，验库存并锁库存
        skuLockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });

        //判断锁定的结果集中包含锁定失败的商品（如果有任何一个商品失败，已经锁定成功的商品应该回滚）
        if(skuLockVos.stream().anyMatch(skuLockVo -> skuLockVo.getLock() == false)){
            //获取已经锁定成功商品，解锁库存
            skuLockVos.stream().filter(skuLockVo -> skuLockVo.getLock()).forEach(skuLockVo -> {
                //解锁库存
                wareSkuDao.unLock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });
            return skuLockVos;
        }
        //把库存的锁定信息保存到redis中，方便获取锁定库存的信息
        String orderToken = skuLockVos.get(0).getOrderToken();

        redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVos));

        //定时释放库存，订时时间（35min）> 关单时间（30min）
        amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.ttl",orderToken);
        return null;
    }

    /**
     * 验库存并锁库存
     * 为保证原子性必须添加分布式锁
     */

    private void checkLock(SkuLockVo skuLockVo){
        RLock fairLock = redissonClient.getFairLock("lock" + skuLockVo.getSkuId());
        fairLock.lock();
        //验库存
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.check(skuLockVo.getSkuId(), skuLockVo.getCount());
        if(!CollectionUtils.isEmpty(wareSkuEntities)){
            //锁库存  大数据分析以就近的仓库锁库存，这里我们就取第一个仓库锁库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            int lock = wareSkuDao.lock(wareSkuEntity.getId(), skuLockVo.getCount());
            if(lock != 0){
                skuLockVo.setLock(true);
                skuLockVo.setWareSkuId(wareSkuEntity.getId());
            }
        }
        fairLock.unlock();
    }
}