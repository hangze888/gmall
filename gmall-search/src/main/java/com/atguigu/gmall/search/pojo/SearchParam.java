package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String key;

    private Long[] catelog3;

    private Long[] brand;

    private String order;

    private Integer pageNum = 1;
    private Integer pageSize = 64;

    private List<String> props;

    private Double priceFrom;
    private Double priceTo;

    private Boolean store;
}
