package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.vo.BaseGroupVo;
import com.atguigu.gmall.sms.vo.SaleVoS;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVo {

    private Long skuId;

    private Long spuId;
    private String spuName;

    private Long categoryId;
    private String categoryName;

    private Long brandId;
    private String brandName;

    private String skuTitle;
    private String skuSubTitle;

    private BigDecimal price;
    private BigDecimal weight;

    private Boolean store;//库存信息
    //详情介绍
    private List<String> desc;
    //商品的图片
    private List<SkuImagesEntity> images;
    //sku促销信息
    private List<SaleVoS> sales;
    //spu的基本属性(组及组下的规格参数)
    private List<BaseGroupVo> attrGroups;
    //sku的所有销售属性的组合
    private List<SkuSaleAttrValueEntity> saleAttrs;

}
