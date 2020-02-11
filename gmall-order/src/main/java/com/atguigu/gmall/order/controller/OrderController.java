package com.atguigu.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.PayAsyncVo;
import com.atguigu.gmall.order.vo.PayVo;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo orderConfirmVo = orderService.confirm();
        return Resp.ok(orderConfirmVo);
    }

    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo orderSubmitVo){
        OrderEntity orderEntity = orderService.submit(orderSubmitVo);
        if(orderEntity != null){
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getTotalAmount().toString());
            payVo.setSubject("谷粒商城");
            payVo.setBody("谷粒商城支付页面");
            try {
                String form = alipayTemplate.pay(payVo);
                System.out.println(form);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }
        }
        return Resp.ok(null);
    }

    /**
     * 支付宝支付成功后的异步回调接口
     * 想让别人回调你的接口：
     * 1.自己的独立ip（电信公司：企业专线）
     * 2.买域名
     * 这里解决方案：内网穿透（花生壳/哲西云，众筹）
     * @return
     */
    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){

        //修改订单状态
        amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.pay", payAsyncVo.getOut_trade_no());
        return Resp.ok(null);
    }

    /**
     * 秒杀
     * 减redis中的库存
     * 发送消息异步创建订单
     * @param skuId
     * @return
     */
    @PostMapping("seckill/{skuId}")
    public Resp<Object> seckill(@PathVariable("skuId") Long skuId) {

        String num = redisTemplate.opsForValue().get("seckill:num" + skuId);

        try {
            RSemaphore semaphore = redissonClient.getSemaphore("seckill" + skuId);
            semaphore.trySetPermits(Integer.valueOf(num));
            semaphore.acquire(1);

            this.redisTemplate.opsForValue().decrement("seckill:num" + skuId);

            UserInfo userInfo = LoginInterceptor.getUserInfo();
            String timeId = IdWorker.getTimeId();
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setOrderToken(timeId);
            skuLockVo.setCount(1);
            //     skuLockVo.setUserId

            this.amqpTemplate.convertAndSend("order-exchange", "order:seckill", skuLockVo);

            RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("seckill:latch" + userInfo.getUserId());
            countDownLatch.trySetCount(1);

            //在oms微服务中创建订单完成之后调用countdown方法
            semaphore.release();

            return Resp.ok(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Resp.fail("秒杀失败");
        }
    }

    /**
     * 查看秒杀订单
     */
    @GetMapping
    public Resp<Object> querySeckill() throws InterruptedException {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("seckill:latch" + userInfo.getUserId());
        countDownLatch.await();

        //查询订单业务流程
        return Resp.ok(null);
    }
}
