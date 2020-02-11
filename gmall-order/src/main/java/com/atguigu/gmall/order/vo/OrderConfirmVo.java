package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<MemberReceiveAddressEntity> address;

    private List<OrderItemVo> orderItems;

    private Integer bounds;//积分信息

    private String orderToken;//防止重复提交
}
