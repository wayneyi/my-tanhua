package com.tanhua.server.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.common.mapper.SettingsMapper;
import com.tanhua.common.pojo.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SettingsService {

    @Autowired
    private SettingsMapper settingsMapper;

    /**
     * 根据用户id查询配置
     * 
     * @param userId
     * @return
     */
    public Settings querySettings(Long userId) {
        QueryWrapper<Settings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.settingsMapper.selectOne(queryWrapper);
    }

    public Boolean updateNotification(Long userId, Boolean likeNotification,
                                      Boolean pinglunNotification, Boolean gonggaoNotification) {
        //查询用户的设置
        Settings settings = querySettings(userId);
        if(ObjectUtil.isEmpty(settings)){
            settings = new Settings();
            settings.setUserId(userId);
            settings.setGonggaoNotification(gonggaoNotification);
            settings.setLikeNotification(likeNotification);
            settings.setPinglunNotification(pinglunNotification);
            settings.setCreated(new Date());
            settings.setUpdated(settings.getCreated());

            settingsMapper.insert(settings);
        }else{
            settings.setGonggaoNotification(gonggaoNotification);
            settings.setLikeNotification(likeNotification);
            settings.setPinglunNotification(pinglunNotification);
            settings.setUpdated(settings.getCreated());

            settingsMapper.updateById(settings);
        }

        return true;
    }
}