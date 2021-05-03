package com.tanhua.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.common.mapper.UserInfoMapper;
import com.tanhua.common.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class UserInfoService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    public UserInfo queryUserInfoByUserId(Long userId) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return userInfoMapper.selectOne(queryWrapper);
    }

    public List<UserInfo> queryUserInfoList(QueryWrapper<UserInfo> queryWrapper) {
        return userInfoMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户id列表查询用户信息
     * @param userIdList
     * @return
     */
    public List<UserInfo> queryUserInfoByUserIdList(Collection<?> userIdList) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIdList);
        return queryUserInfoList(queryWrapper);
    }

    public boolean updateUserInfoByUserId(UserInfo userInfo) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userInfo.getUserId());
        return this.userInfoMapper.update(userInfo, queryWrapper) > 0;
    }
}
