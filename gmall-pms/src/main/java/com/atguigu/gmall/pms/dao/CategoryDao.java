package com.atguigu.gmall.pms.dao;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品三级分类
 * 
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-01-01 23:05:25
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {

    List<CategoryVo> queryCategoriesWithSub(Long pid);
}
