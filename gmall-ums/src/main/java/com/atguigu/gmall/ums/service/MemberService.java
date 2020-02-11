package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.sun.org.apache.xpath.internal.operations.Bool;


/**
 * 会员
 *
 * @author hangze
 * @email lxf@atguigu.com
 * @date 2020-01-15 16:38:56
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean check(String data, Integer type);

    void register(MemberEntity memberEntity,String code);

    MemberEntity queryUser(String username, String password);
}

