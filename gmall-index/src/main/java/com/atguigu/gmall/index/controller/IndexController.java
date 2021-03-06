package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("index")
public class IndexController {
    
    @Autowired
    private IndexService indexService;

    @GetMapping("cates")
    public Resp<List<CategoryEntity>> queryLvl1Categories(){
        List<CategoryEntity> categoryEntities = indexService.queryLvl1Categories();
        return Resp.ok(categoryEntities);
    }

    @GetMapping("cates/{pid}")
    public Resp<List<CategoryVo>> queryCategoriesWithSub(@PathVariable("pid")Long pid){
        List<CategoryVo> categoryVos = indexService.queryCategoriesWithSub(pid);
        return Resp.ok(categoryVos);
    }

    @GetMapping("catelock")
    public Resp<Object> testLock(){
        indexService.testLock();
        return Resp.ok(null);
    }

    @GetMapping("cateRead")
    public Resp<String> testRead(){
        String msg = indexService.testRead();
        return Resp.ok(msg);
    }

    @GetMapping("cateWrite")
    public Resp<String> testWrite(){
        String msg = indexService.testWrite();
        return Resp.ok(msg);
    }

    @GetMapping("testLatch")
    public Resp<String> testLatch() throws InterruptedException {
        String msg = indexService.testLatch();
        return Resp.ok(msg);
    }

    @GetMapping("testCountDown")
    public Resp<String> testCountDown(){
        String msg = indexService.testCountDown();
        return Resp.ok(msg);
    }
}
