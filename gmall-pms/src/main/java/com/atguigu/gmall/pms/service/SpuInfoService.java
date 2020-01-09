package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuInfoVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * spu信息
 *
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-01-01 23:05:25
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageVo queryPage(QueryCondition params);


    PageVo querySupInfoByCid(Long cid,QueryCondition queryCondition);

    void bigSave(SpuInfoVO spuInfoVO);


}

