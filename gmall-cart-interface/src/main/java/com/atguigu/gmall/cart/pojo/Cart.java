package com.atguigu.gmall.cart.pojo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.SaleVoS;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Cart {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs;
    private BigDecimal price;
    private BigDecimal currentPrice;//当前价格
    private Integer count;
    private Boolean store = true;//库存
    //选中状态
    private Boolean check;
    //促销信息
    private List<SaleVoS> sales;
}
