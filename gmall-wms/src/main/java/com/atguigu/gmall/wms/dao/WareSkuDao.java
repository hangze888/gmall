package com.atguigu.gmall.wms.dao;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-01-02 00:05:59
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}
