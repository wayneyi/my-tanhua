package com.tanhua.sso.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tanhua.common.pojo.User;
import com.tanhua.sso.vo.ErrorResult;
import io.lettuce.core.RedisCommandTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MyCenterService {
    @Autowired
    private UserService userService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public boolean sendVerificationCode(String token) {
        //校验token
        User user = userService.queryUserByToken(token);
        if(ObjectUtil.isEmpty(user)){
            return false;
        }

        ErrorResult errorResult = smsService.sendCheckCode(user.getMobile());

        return "000003".equals(errorResult.getErrCode());
    }

    public Boolean checkVerificationCode(String code, String token) {
        //校验token
        User user = userService.queryUserByToken(token);
        if(ObjectUtil.isEmpty(user)){
            return false;
        }

        //校验验证码,先查询redis中的验证码
        String redisKey = "CHECK_CODE_" + user.getMobile();
        String value = redisTemplate.opsForValue().get(redisKey);
        if(StrUtil.equals(code,value)){
            //将验证码删除
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    public boolean updatePhone(String token, String newPhone) {
        //校验token
        User user = userService.queryUserByToken(token);
        if(ObjectUtil.isEmpty(user)){
            return false;
        }

        return userService.updatePhone(user.getId(),newPhone);
    }
}
