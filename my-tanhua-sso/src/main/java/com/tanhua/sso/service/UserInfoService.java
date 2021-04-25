package com.tanhua.sso.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.sso.enums.SexEnum;
import com.tanhua.sso.mapper.UserInfoMapper;
import com.tanhua.sso.pojo.User;
import com.tanhua.sso.pojo.UserInfo;
import com.tanhua.sso.vo.PicUploadResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UserInfoService {
    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private FaceEngineService faceEngineService;

    @Autowired
    private PicUploadService picUploadService;

    public Boolean saveUserInfo(Map<String, String> param, String token) {
        //校验token
        User user = userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setSex(StringUtils.equalsIgnoreCase(param.get("gender"), "man") ? SexEnum.MAN : SexEnum.WOMAN);
        userInfo.setNickName(param.get("nickname"));
        userInfo.setBirthday(param.get("birthday"));
        userInfo.setCity(param.get("city"));

        return userInfoMapper.insert(userInfo) == 1;
    }

    public Boolean saveUserLogo(MultipartFile file, String token) {
        //校验token
        User user = userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        try {
            //校验图片是否是人像,如果不是人像返回false
            boolean b = faceEngineService.checkIsPortrait(file.getBytes());
            if(!b){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //图片上传到阿里云oss
        PicUploadResult result = picUploadService.upload(file);
        if(StringUtils.isEmpty(result.getName())){
            //上传失败
            return false;
        }

        //吧头像保存到用户信息表中
        UserInfo userInfo = new UserInfo();
        userInfo.setLogo(result.getName());

        QueryWrapper<UserInfo> query = new QueryWrapper<>();
        query.eq("user_id", user.getId());

        return userInfoMapper.update(userInfo,query) == 1;
    }
}
