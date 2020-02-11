package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.vo.SaleVo;
import com.atguigu.gmall.sms.vo.SaleVoS;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuLadderDao skuLadderDao;
    @Autowired
    private SkuFullReductionDao skuFullReductionDao;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Override
    @Transactional
    public void saveSales(SaleVo saleVo) {
        //3.1 sku积分信息
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo,skuBoundsEntity);
        List<String> works = saleVo.getWork();
        skuBoundsEntity.setWork(new Integer(works.get(0)) + new Integer(works.get(1))*2 + new Integer(works.get(2))*4 + new Integer(works.get(3))*8);
        this.save(skuBoundsEntity);
        //3.2 sku打折信息
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(saleVo.getLadderaddOther());
        skuLadderDao.insert(skuLadderEntity);
        //3.3 满减信息
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(saleVo.getAddOther());
        skuFullReductionDao.insert(skuFullReductionEntity);
    }

    @Override
    public List<SaleVoS> queryItemSaleVoBySkuId(Long skuId) {
        List<SaleVoS> itmeSales = new ArrayList<>();

        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity != null){
            SaleVoS saleVoS = new SaleVoS();
            saleVoS.setType("积分");
            saleVoS.setDesc("赠送"+skuBoundsEntity.getGrowBounds()+"成长积分，"+skuBoundsEntity.getBuyBounds()+"购物积分");
            itmeSales.add(saleVoS);
        }

        SkuLadderEntity skuLadderEntity = skuLadderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(skuLadderEntity != null){
            SaleVoS saleVoS = new SaleVoS();
            saleVoS.setType("打折");
            saleVoS.setDesc("满"+skuLadderEntity.getFullCount()+"打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itmeSales.add(saleVoS);
        }

        SkuFullReductionEntity skuFullReductionEntity = skuFullReductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if(skuFullReductionEntity != null){
            SaleVoS saleVoS = new SaleVoS();
            saleVoS.setType("满减");
            saleVoS.setDesc("满"+skuFullReductionEntity.getFullPrice()+"减"+skuFullReductionEntity.getReducePrice());
            itmeSales.add(saleVoS);
        }
        return itmeSales;
    }

}