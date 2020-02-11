package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.dao.OrderDao;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderListener {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @RabbitListener(queues = {"ORDER-DEAD-QUEUE"})
    public void closeOrder(String orderToKen){
        //关闭订单
        if(orderDao.closeOrder(orderToKen) == 1){
            //解锁库存
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.unlock",orderToKen);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-PAY-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.pay"}
    ))
    public void payOrder(String orderToken){
        //1.更新订单的状态
        int i = orderDao.updatePayStatus(orderToken);
        if(i == 1){
            //2.真正的减库存操作
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.minus",orderToken);
            //3.给用户添加积分
        }
    }
}
