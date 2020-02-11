package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrValueVo;
import com.atguigu.gmall.pms.vo.SkuInfoVO;
import com.atguigu.gmall.pms.vo.SpuInfoVO;
import com.atguigu.gmall.sms.vo.SaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import org.springframework.util.CollectionUtils;




@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescDao spuInfoDescDao;
    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private SkuInfoDao skuInfoDao;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }


    @Override
    public PageVo querySupInfoByCid(Long cid,QueryCondition queryCondition) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        // 判断查全站，查本类
        if(cid != 0l){
            wrapper.eq("catalog_id",cid);
        }
        //查关键字
        String key = queryCondition.getKey();
        if(StringUtils.isNotBlank(key)){
            wrapper.and(t->t.eq("id",key).or().like("spu_name",key));
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(queryCondition),
                wrapper
        );

        return new PageVo(page);
    }

    @Override
    @GlobalTransactional
    public void bigSave(SpuInfoVO spuInfoVO) {

        //保存spu相关信息
        //1.1 spuInfo
        Long spuId = saveSpuInfo(spuInfoVO);
        //1.2 spuInfoDesc
        spuInfoDescService.saveSpuInfoDesc(spuInfoVO, spuId);
        //1.3 基础属性相关信息
        saveBaseAttrValue(spuInfoVO, spuId);
        //sku相关信息
        saveSkuInfo(spuInfoVO, spuId);

        sendMsg(spuId,"insert");
    }

    private void sendMsg(Long spuId,String type) {
        amqpTemplate.convertAndSend("GMALL-PMS-EXCHANGE","item."+type,spuId);
    }

    private void saveSkuInfo(SpuInfoVO spuInfoVO, Long spuId) {
        List<SkuInfoVO> skus = spuInfoVO.getSkus();
        if(!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku->{
                // 2.1 skuInfo
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setSpuId(spuId);
                List<String> images = sku.getImages();
                if(!CollectionUtils.isEmpty(images)){
                    //如果将来用户传一个默认图片
                    skuInfoEntity.setSkuDefaultImg(skuInfoEntity.getSkuDefaultImg() == null?images.get(0):skuInfoEntity.getSkuDefaultImg());
                }
                skuInfoEntity.setBrandId(spuInfoVO.getBrandId());//品牌id
                skuInfoEntity.setCatalogId(spuInfoVO.getCatalogId());//分类id
                skuInfoEntity.setSkuCode(UUID.randomUUID().toString());
                skuInfoDao.insert(skuInfoEntity);
                Long skuId = skuInfoEntity.getSkuId();
                //2.2 skuInfoImages
                if(!CollectionUtils.isEmpty(images)){
                    List<SkuImagesEntity> skuImagesEntities = images.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setImgUrl(image);
                        skuImagesEntity.setImgSort(0);
                        skuImagesEntity.setDefaultImg(StringUtils.equals(image, skuInfoEntity.getSkuDefaultImg()) ? 1 : 0);
                        return skuImagesEntity;
                    }).collect(Collectors.toList());
                    skuImagesService.saveBatch(skuImagesEntities);
                }
                //2.3 skuSaleAttrValue
                List<SkuSaleAttrValueEntity> saleAttrs = sku.getSaleAttrs();
                if(!CollectionUtils.isEmpty(saleAttrs)){
                    saleAttrs.forEach(SkuSaleAttrValueEntity->{
                        SkuSaleAttrValueEntity.setSkuId(skuId);
                        SkuSaleAttrValueEntity.setAttrSort(0);
                    });
                    skuSaleAttrValueService.saveBatch(saleAttrs);
                }
                //营销相关信息
                SaleVo saleVo = new SaleVo();
                BeanUtils.copyProperties(sku,saleVo);
                saleVo.setSkuId(skuId);
                smsClient.saveSales(saleVo);
            });
        }
    }

    private void saveBaseAttrValue(SpuInfoVO spuInfoVO, Long spuId) {
        List<BaseAttrValueVo> baseAttrs = spuInfoVO.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)){
            List<ProductAttrValueEntity> attrValues = baseAttrs.stream().map(baseAttrValueVo -> {
                ProductAttrValueEntity attrValueEntities = new ProductAttrValueEntity();
                BeanUtils.copyProperties(baseAttrValueVo, attrValueEntities);
                attrValueEntities.setSpuId(spuId);
                attrValueEntities.setAttrSort(0);
                attrValueEntities.setQuickShow(0);
                return attrValueEntities;
            }).collect(Collectors.toList());
            productAttrValueService.saveBatch(attrValues);
        }
    }

    private Long saveSpuInfo(SpuInfoVO spuInfoVO) {
        spuInfoVO.setCreateTime(new Date());
        spuInfoVO.setUodateTime(spuInfoVO.getCreateTime());
        this.save(spuInfoVO);
        return spuInfoVO.getId();
    }
}