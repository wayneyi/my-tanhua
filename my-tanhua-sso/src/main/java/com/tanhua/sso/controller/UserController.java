package com.tanhua.sso.controller;

import com.tanhua.common.pojo.User;
import com.tanhua.sso.service.UserService;
import com.tanhua.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("loginVerification")
    public ResponseEntity<Object> login(@RequestBody Map<String,String> param){
        String data = null;
        try {
            String phone = param.get("phone");
            String code = param.get("verificationCode");

            data = userService.login(phone,code);

            if(StringUtils.equals("1", data)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("验证码不对");
            }else if(StringUtils.equals("2", data)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("验证码失效");
            }else {
                //登录成功
                String[] s = StringUtils.split(data,"|");
                Map<String,Object> result = new HashMap<>();
                result.put("token", s[1]);
                result.put("isNew", Boolean.valueOf(s[0]));
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ErrorResult errorResult = ErrorResult.builder().errCode("000002").errMessage("登录失败！").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    /**
     * 校验token,根据token查询用户数据
     * @param token
     * @return
     */
    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token){
        return userService.queryUserByToken(token);
    }
}
