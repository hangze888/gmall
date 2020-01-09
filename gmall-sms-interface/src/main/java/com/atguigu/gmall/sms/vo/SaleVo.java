package com.atguigu.gmall.sms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleVo {

    private Long skuId;
    //积分信息
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<String> work;

    //满减信息
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer addOther;

    //打折信息
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderaddOther;
}
