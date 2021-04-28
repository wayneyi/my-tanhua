package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.RecommendUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestRecommendUser {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void test1(){
        Query query = Query.query(Criteria.where("toUserId").is(1))
                .with(Sort.by(Sort.Order.desc("score"))).limit(1);
        RecommendUser one = mongoTemplate.findOne(query, RecommendUser.class);
        System.out.println(one);
    }
}
