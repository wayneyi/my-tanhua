package com.tanhua.sso.controller;

import com.tanhua.sso.service.SmsService;
import com.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("user")
@Slf4j
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("login")
    public ResponseEntity<ErrorResult> sendCheckCode(@RequestBody Map<String,String> param){
        ErrorResult errorResult = null;
        String phone = param.get("phone");

        try{
            errorResult = smsService.sendCheckCode(phone);
            if("000003".equals(errorResult.getErrCode())){  //发送成功
                return ResponseEntity.ok(errorResult);
            }else if("000001".equals(errorResult.getErrCode())){    //上一次发送未失效
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
            }
        }catch (Exception e){
            log.error("发送短信验证码失败~ phone = " + phone,e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }
}
