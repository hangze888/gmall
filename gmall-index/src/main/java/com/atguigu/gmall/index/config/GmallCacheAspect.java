package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        Class returnType = method.getReturnType();
        List<Object> args = Arrays.asList(joinPoint.getArgs());

        //1.获取缓存中的数据
        String priex = gmallCache.value();
        String key = priex + args;
        String jsonString = redisTemplate.opsForValue().get(key);
        //2.判断数据是否为空
        if(StringUtils.isNotBlank(jsonString)){
            return JSON.parseObject(jsonString,returnType);
        }
        //3.加分布式锁
        String lockName = gmallCache.lockName();
        RLock lock = redissonClient.getFairLock(lockName + args);

        //4.在判断缓存是否为空
        String jsonString2 = redisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(jsonString2)){
            lock.unlock();
            return JSON.parseObject(jsonString2,returnType);
        }
        //5.执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        //把数据放入缓存
        redisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+new Random().nextInt(gmallCache.bound()), TimeUnit.MINUTES);
        //释放锁
        lock.unlock();

        return result;
    }
}
