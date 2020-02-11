package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.fegin.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    private static final String PREFIX_KEY = "index:cates";


    public List<CategoryEntity> queryLvl1Categories(){
        Resp<List<CategoryEntity>> listResp = pmsClient.queryCatgoriesByLevelOrPid(1, null);
        List<CategoryEntity> categoryEntities = listResp.getData();
        return categoryEntities;
    }

    @GmallCache(value = "index:cates", timeout = 7200,bound = 5,lockName = "lock")
    public List<CategoryVo> queryCategoriesWithSub(Long pid) {

//        //从缓存中获取数据
//        String cateJson = stringRedisTemplate.opsForValue().get(PREFIX_KEY + pid);
//        //判断数据是否为空
//        if(StringUtils.isNotBlank(cateJson)){
//            return JSON.parseArray(cateJson, CategoryVo.class);
//        }
//        RLock lock = redissonClient.getLock("lock"+pid);
//        lock.lock();
//        //当一个已经查询出来了，在判断一次缓存中有没有，有就直接返回
//        String cateJson2 = stringRedisTemplate.opsForValue().get(PREFIX_KEY + pid);
//        if(StringUtils.isNotBlank(cateJson2)){
//            //返回之前别忘了释放锁
//            lock.unlock();
//            return JSON.parseArray(cateJson2, CategoryVo.class);
//        }
        //如果缓存中没有查询数据库
        Resp<List<CategoryVo>> categoriesWithSub = pmsClient.queryCategoriesWithSub(pid);
        List<CategoryVo> categoryVos = categoriesWithSub.getData();

//        lock.unlock();
//        //把查询的数据放到缓存
//        stringRedisTemplate.opsForValue().set(PREFIX_KEY + pid,JSON.toJSONString(categoryVos), 5 + new Random().nextInt(5),TimeUnit.DAYS);
        return categoryVos;
    }





    public void testLock() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        String value = stringRedisTemplate.opsForValue().get("num");
        if(value == null){
            return;
        }
        Integer numValue = new Integer(value);
        stringRedisTemplate.opsForValue().set("num",String.valueOf(++numValue));
        lock.unlock();
    }


    public void testLock1() {
        String uuid = UUID.randomUUID().toString();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 8, TimeUnit.SECONDS);
        if(flag){
            String value = stringRedisTemplate.opsForValue().get("num");

            if(value == null){
                return;
            }

            Integer numValue = new Integer(value);
            stringRedisTemplate.opsForValue().set("num",String.valueOf(++numValue));
            //执行完业务逻辑要释放锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
//            String lock = stringRedisTemplate.opsForValue().get("lock");
//            if(StringUtils.equals(lock,uuid)){
//                stringRedisTemplate.delete("lock");
//            }
        }else{
            //如果没有获取到锁，重试
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String testRead() {
        RReadWriteLock rwLcok = redissonClient.getReadWriteLock("rwlcok");
        rwLcok.readLock().lock(10, TimeUnit.SECONDS);
        String msg = stringRedisTemplate.opsForValue().get("msg");
        return "读取了一条信息" + msg;
    }

    public String testWrite() {
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("rwlock");
        rwlock.writeLock().lock(10,TimeUnit.SECONDS);
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("msg",uuid);
        return "写了一条信息" + uuid;
    }

    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);
        latch.await();
        return "班长最后走";
    }

    public String testCountDown() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("latch");
        latch.countDown();
        return "数量减一";
    }
}
