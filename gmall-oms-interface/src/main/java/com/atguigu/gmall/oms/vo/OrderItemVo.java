package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVoS;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs;//销售属性
    private BigDecimal price;
    private Integer count;
    private Boolean store = true;//库存
    //促销信息
    private List<SaleVoS> sales;
    //重量
    private BigDecimal weight;


}
