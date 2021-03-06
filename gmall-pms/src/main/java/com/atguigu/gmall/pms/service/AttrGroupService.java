package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.BaseGroupVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-01-01 23:05:25
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByCidPage(QueryCondition queryCondition, Long catId);

    GroupVo queryGroupVOByGid(Long gid);

    List<GroupVo> queryGroupVOsByCid(Long cid);

    List<BaseGroupVo> queryItemGroupVoByCidSpuId(Long cid, Long spuId);
}

