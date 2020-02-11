package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import lombok.Data;

import java.util.List;

@Data
public class BaseGroupVo {

    private Long id;

    private String name;

    private List<ProductAttrValueEntity> attrs;
}
