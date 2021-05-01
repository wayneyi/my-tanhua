package com.tanhua.sso.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.common.mapper.UserMapper;
import com.tanhua.common.pojo.User;
import com.tanhua.dubbo.server.api.HuanXinApi;
import io.jsonwebtoken.ExpiredJwtException;
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
    @Reference(version = "1.0.0")
    private HuanXinApi huanXinApi;

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
        query.eq("mobile", phone);  //根据mobile字段查询user
        User user = userMapper.selectOne(query);

        if (null == user) {
            //需要注册该用户
            user = new User();
            user.setMobile(phone);
            user.setPassword(DigestUtils.md5Hex("123456"));
            //注册新用户
            userMapper.insert(user);
            Long id = user.getId();
            isNew = true;

            //将该用户注册到环信平台
            Boolean register = huanXinApi.register(id);
            if(!register){
                log.error("注册环信失败!"+user.getId());
            }
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

    public User queryUserByToken(String token) {
        try {
            //通过token解析数据
            Map<String, Object> body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody(); //{id=2,exp=.....}

            User user = new User();
            user.setId(Long.valueOf(body.get("id").toString()));

            //需要返回user对象中的mobile,需要查询数据库获取mobile数据
            //如果每次都查询数据库,会影响性能,需要对mobile数据进行缓存
            //数据缓存时,需要设置过期时间,过期时间要和token的一致

            return user;
        } catch (ExpiredJwtException e) {
            log.error("Token已过期 token=" + token, e);
        } catch (Exception e) {
            log.error("Token不合法 token=" + token, e);
        }
        return null;
    }
}
