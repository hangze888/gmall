package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 订单
 *
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-02-09 13:28:42
 */
public interface OrderService extends IService<OrderEntity> {

    PageVo queryPage(QueryCondition params);

    OrderEntity saveOrder(OrderSubmitVo orderSubmitVo, Long userId);
}

