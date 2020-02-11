package com.atguigu.gmall.index.config;


import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    //自定义缓存中key的前缀
    String value() default "";

    //自定一缓存的有效时间  分钟
    int timeout() default 30;

    //防止雪崩，设置随机范围值
    int bound() default 5;

    //自定义分布锁的名称
    String lockName() default "lock";
}
