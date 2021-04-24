package com.tanhua.sso.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestSms {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testSendSms(){
        redisTemplate.opsForValue().set("CHECK_CODE_17507331722","111111",Duration.ofMillis(5));
    }
}
