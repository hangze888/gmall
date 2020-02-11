package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PmsListener {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GoodRepository repository;
    @Autowired
    private GmallWmsClient wmsClient;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL-SEARCH-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId){
        Resp<List<SkuInfoEntity>> skuResp = pmsClient.querySkuInfoBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
        if(!CollectionUtils.isEmpty(skuInfoEntities)){
            List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                Goods goods = new Goods();
                //查询sku库存信息
                Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.queryWareSkuBySkuId(skuInfoEntity.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                if(!CollectionUtils.isEmpty(wareSkuEntities)){
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                }

                goods.setSkuId(skuInfoEntity.getSkuId());
                goods.setSale(10l);
                goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                //后来添加的
                Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(spuId);
                SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                goods.setCreateTime(spuInfoEntity.getCreateTime());

                Resp<BrandEntity> brandEntityResp = pmsClient.queryBrandById(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if(brandEntity != null){
                    goods.setBrandId(skuInfoEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                }

                Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if(categoryEntity != null){
                    goods.setCategoryId(skuInfoEntity.getCatalogId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                Resp<List<ProductAttrValueEntity>> attrValueResp = pmsClient.querySearchAttrValue(spuId);
                List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                List<SearchAttrValue> searchAttrValues = attrValueEntities.stream().map(attrValueEntity -> {
                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                    searchAttrValue.setAttrId(attrValueEntity.getAttrId());
                    searchAttrValue.setAttrName(attrValueEntity.getAttrName());
                    searchAttrValue.setAttrValue(attrValueEntity.getAttrValue());
                    return searchAttrValue;
                }).collect(Collectors.toList());
                goods.setAttrs(searchAttrValues);

                goods.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                goods.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
                goods.setSkuTitle(skuInfoEntity.getSkuTitle());

                return goods;
            }).collect(Collectors.toList());
            repository.saveAll(goodsList);
        }
    }
}
