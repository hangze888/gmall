package com.atguigu.gmall.oms.dao;

import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-02-09 13:28:42
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
