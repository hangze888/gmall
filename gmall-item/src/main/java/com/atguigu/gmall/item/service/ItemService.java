package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.BaseGroupVo;
import com.atguigu.gmall.sms.vo.SaleVoS;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    public ItemVo queryItemVo(Long skuId) {
        ItemVo itemVo = new ItemVo();
        //根据skuId查询sku
        itemVo.setSkuId(skuId);
        CompletableFuture<SkuInfoEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return null;
            }
            itemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVo.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVo.setPrice(skuInfoEntity.getPrice());
            itemVo.setWeight(skuInfoEntity.getWeight());
            return skuInfoEntity;
        },threadPoolExecutor);

        //根据sku中的categoryId查询分类
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {
                itemVo.setCategoryId(categoryEntity.getCatId());
                itemVo.setCategoryName(categoryEntity.getName());
            }
        },threadPoolExecutor);

        //根据sku中的brandId查询品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = pmsClient.queryBrandById(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getBrandId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPoolExecutor);

        //根据skuId查询spu的信息
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVo.setSpuId(spuInfoEntity.getId());
                itemVo.setSpuName(spuInfoEntity.getSpuName());
            }
        },threadPoolExecutor);

        //根据skuId查询图片
        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> imagesResp = pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResp.getData();
            if (!CollectionUtils.isEmpty(skuImagesEntities)) {
                itemVo.setImages(skuImagesEntities);
            }
        },threadPoolExecutor);

        //根据spuId查询商品描述的信息
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = pmsClient.querySpuDescBySpuId(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null && StringUtils.isNotBlank(spuInfoDescEntity.getDecript())) {
                itemVo.setDesc(Arrays.asList(StringUtils.split(spuInfoDescEntity.getDecript(), ",")));
            }
        },threadPoolExecutor);

        //根据skuId查询库存的信息
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> storeResp = wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = storeResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
        },threadPoolExecutor);

        //根据skuId查询促销信息
        CompletableFuture<Void> saleVoCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SaleVoS>> SaleVoResp = smsClient.queryItemSaleVoBySkuId(skuId);
            List<SaleVoS> saleVoSList = SaleVoResp.getData();
            itemVo.setSales(saleVoSList);
        },threadPoolExecutor);


        // 1.根据sku中的spuId查询skus
        // 2.根据skus获取skuIds
        // 3.根据skuIds查询销售属性

        CompletableFuture<Void> attrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<SkuSaleAttrValueEntity>> saleAttrValueBySpuId = pmsClient.querySaleAttrValueBySpuId(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> attrValueBySpuIdData = saleAttrValueBySpuId.getData();
            itemVo.setSaleAttrs(attrValueBySpuIdData);
        },threadPoolExecutor);

        // 1.根据sku中的categoryId查询分组
        // 2.遍历组到中间表中查询每个组的规格参数id
        // 3.根据spuId和attrId查询规格参数名及值

        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<BaseGroupVo>> groupVoByCidSpuId = pmsClient.queryItemGroupVoByCidSpuId(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<BaseGroupVo> baseGroupVoList = groupVoByCidSpuId.getData();
            itemVo.setAttrGroups(baseGroupVoList);
        },threadPoolExecutor);

        CompletableFuture.allOf(categoryCompletableFuture,brandCompletableFuture,spuCompletableFuture,descCompletableFuture, attrCompletableFuture,groupCompletableFuture,
                                imageCompletableFuture,storeCompletableFuture,saleVoCompletableFuture).join();

        return itemVo;
    }
}
