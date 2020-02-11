package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseVO;
import org.springframework.stereotype.Service;

import java.io.IOException;

public interface SearchService {

    public SearchResponseVO search(SearchParam searchParam) throws IOException;
}
