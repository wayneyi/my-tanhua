package com.tanhua.sso.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.sso.mapper.UserMapper;
import com.tanhua.sso.pojo.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${jwt.secret}")
    private String secret;

    /**
     * 用户登录
     *
     * @param phone 手机号
     * @param code  验证码
     * @return
     */
    public String login(String phone, String code) {
        String redisKey = "CHECK_CODE_" + phone;
        String redisData = redisTemplate.opsForValue().get(redisKey);

        //返回结果
        String result = null;

        if (StringUtils.isEmpty(redisData)) {
            //验证码失效
            result = "2";
            return result;
        }

        if (!StringUtils.equals(code, redisData)) {
            //验证码不对
            result = "1";
            return result;
        }

        //验证码在校验完成后，需要废弃
        redisTemplate.delete(redisKey);

        //查询数据库,看用户是否已经注册
        boolean isNew = false;  //判断是否新用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("mobile", phone);  //查询条件mobile字段
        User user = userMapper.selectOne(query);

        if (null == user) {
            //需要注册该用户
            user = new User();
            user.setMobile(phone);
            user.setPassword(DigestUtils.md5Hex("123456"));
            //注册新用户
            userMapper.insert(user);
            isNew = true;
        }

        //生成token
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("id", user.getId());

        // 生成token
        String token = Jwts.builder()
                .setClaims(claims) //payload，存放数据的位置，不能放置敏感数据，如：密码等
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .setExpiration(new DateTime().plusHours(12).toDate()) //设置过期时间，12小时后过期
                .compact();

        try {
            //发送用户登录成功的消息
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", user.getId());
            msg.put("date", System.currentTimeMillis());

            this.rocketMQTemplate.convertAndSend("tanhua-sso-login", msg);
        } catch (MessagingException e) {
            log.error("发送消息失败！", e);
        }

        result = isNew + "|" + token;
        return result;
    }
}
