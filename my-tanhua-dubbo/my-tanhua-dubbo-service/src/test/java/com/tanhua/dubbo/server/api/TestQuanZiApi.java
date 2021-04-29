package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.vo.PageInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestQuanZiApi {
    @Autowired
    private QuanZiApi quanZiApi;

    @Test
    public void testQueryPublishList(){
        PageInfo<Publish> pageInfo = quanZiApi.queryPublishList(1L, 1, 2);
        System.out.println(pageInfo);
        System.out.println("------------------");
        quanZiApi.queryPublishList(1L, 2, 2).getRecords().forEach(System.out::println);
        System.out.println("------------------");
        quanZiApi.queryPublishList(1L, 3, 2).getRecords().forEach(System.out::println);
    }
}
