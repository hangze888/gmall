package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.BaseGroupVo;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpuByPage(@RequestBody QueryCondition queryCondition);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuInfoBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuById(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);

    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCatgoriesByLevelOrPid(@RequestParam(value = "level",defaultValue = "0")Integer level,
                                                                 @RequestParam(value = "parentCid",required = false)Long pid);

    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> queryCategoriesWithSub(@PathVariable("pid")Long pid);

    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> querySearchAttrValue(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpuDescBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/withattrvalues")
    public Resp<List<BaseGroupVo>> queryItemGroupVoByCidSpuId(
            @RequestParam("cid") Long cid,
            @RequestParam("spuId") Long spuId
    );

    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySaleAttrValueBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skusaleattrvalue/sku/{skuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySaleAttrValueSkuId(@PathVariable("skuId") Long skuId);
}
