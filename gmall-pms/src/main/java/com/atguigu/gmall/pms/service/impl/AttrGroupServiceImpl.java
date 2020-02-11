package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.service.ProductAttrValueService;
import com.atguigu.gmall.pms.vo.BaseGroupVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;
    @Autowired
    private AttrDao attrDao;
    @Autowired
    private ProductAttrValueDao productAttrValueDao;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByCidPage(QueryCondition queryCondition, Long catId) {

        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(queryCondition),
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id",catId)
        );
        return new PageVo(page);
    }

    @Override
    public GroupVo queryGroupVOByGid(Long gid) {
        GroupVo groupVo = new GroupVo();
        //根据gid查询哪个组
        AttrGroupEntity groupEntity = this.getById(gid);
        BeanUtils.copyProperties(groupEntity,groupVo);
        //查询中间表
        List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", gid));
        groupVo.setRelations(relations);
        //判断中间表是否为空
        if(CollectionUtils.isEmpty(relations)){
            return groupVo;
        }
        //获取所有的规格参数id
        List<Long> attrIds = relations.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
        // 查询规格参数
        List<AttrEntity> attrEntities = attrDao.selectBatchIds(attrIds);
        groupVo.setAttrEntities(attrEntities);
        return groupVo;
    }

    @Override
    public List<GroupVo> queryGroupVOsByCid(Long cid) {
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        //遍历规格参数组，查询每个组下的中间关系
        return groupEntities.stream().map(AttrGroupEntity-> queryGroupVOByGid(AttrGroupEntity.getAttrGroupId())).collect(Collectors.toList());

//        return groupEntities.stream().map(AttrGroupEntity->{
//            return queryGroupVOByGid(AttrGroupEntity.getAttrGroupId());
//        }).collect(Collectors.toList());
    }

    @Override
    public List<BaseGroupVo> queryItemGroupVoByCidSpuId(Long cid, Long spuId) {
        // 1.根据sku中的categoryId查询分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        if(CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }
        List<BaseGroupVo> baseGroupVos = attrGroupEntities.stream().map(AttrGroupEntity -> {
            BaseGroupVo baseGroupVo = new BaseGroupVo();
            baseGroupVo.setId(AttrGroupEntity.getAttrGroupId());
            baseGroupVo.setName(AttrGroupEntity.getAttrGroupName());
            // 2.遍历组到中间表中查询每个组的规格参数id
            List<AttrAttrgroupRelationEntity> relationEntities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", AttrGroupEntity.getAttrGroupId()));
            if (!CollectionUtils.isEmpty(relationEntities)) {
                List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
                // 3.根据spuId和attrId查询规格参数名及值
                List<ProductAttrValueEntity> attrValueEntities = productAttrValueDao.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                baseGroupVo.setAttrs(attrValueEntities);
            }
            return baseGroupVo;
        }).collect(Collectors.toList());
        return baseGroupVos;
    }
}