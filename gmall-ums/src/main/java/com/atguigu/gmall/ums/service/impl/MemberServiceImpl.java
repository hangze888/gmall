package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberDao memberDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean check(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();

        switch (type){
            case 1: wrapper.eq("username",data); break;
            case 2: wrapper.eq("mobile",data); break;
            case 3: wrapper.eq("email",data); break;
            default: return null;
        }
        return memberDao.selectCount(wrapper) == 0;
    }

    @Override
    public void register(MemberEntity memberEntity,String code) {
        //1.校验验证码是否正确
        String redisCode = stringRedisTemplate.opsForValue().get(memberEntity.getMobile());
        if(!StringUtils.equals(code,redisCode)){
            return;
        }
        //2.生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        memberEntity.setSalt(salt);
        //3.对密码加密
        String password = memberEntity.getPassword();
        password = DigestUtils.md5Hex(salt+password);
        memberEntity.setPassword(password);
        //4.设置创建时间
        memberEntity.setCreateTime(new Date());
        //5.保存到数据库
        boolean b = this.save(memberEntity);
        //6.注册成功，删除redis中的记录
        if(b){
            stringRedisTemplate.delete(memberEntity.getMobile());
        }
    }

    @Override
    public MemberEntity queryUser(String username, String password) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<MemberEntity>().eq("username", username);
        MemberEntity memberEntity = this.getOne(wrapper);
        //校验用户名
        if(memberEntity == null){
            return memberEntity;
        }
        //校验密码
        String salt = memberEntity.getSalt();
        password = DigestUtils.md5Hex(salt + password);
        if(!StringUtils.equals(password,memberEntity.getPassword())){
            return null;
        }

        return memberEntity;
    }
}