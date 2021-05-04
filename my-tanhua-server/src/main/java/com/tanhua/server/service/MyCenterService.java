package com.tanhua.server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.common.enums.SexEnum;
import com.tanhua.common.pojo.*;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.api.UserLikeApi;
import com.tanhua.dubbo.server.api.VisitorsApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.UserLike;
import com.tanhua.dubbo.server.pojo.Visitors;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MyCenterService {

    @Autowired
    private UserInfoService userInfoService;

    @Reference(version = "1.0.0")
    private UserLikeApi userLikeApi;

    @Reference(version = "1.0.0")
    private VisitorsApi visitorsApi;

    @Autowired
    private IMService imService;

    @Autowired
    private TanHuaService tanHuaService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private BlackListService blackListService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private RecommendUserService recommendUserService;

    public UserInfoVo queryUserInfoByUserId(Long userId) {
        if (ObjectUtil.isEmpty(userId)) {
            //如果查询id为null，就表示查询当前用户信息
            userId = UserThreadLocal.get().getId();
        }
        //查询用户信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(userId);
        if (ObjectUtil.isEmpty(userInfo)) {
            return null;
        }

        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class, "marriage");
//        userInfoVo.setGender(userInfo.getSex().toString().toLowerCase());
        userInfoVo.setGender(userInfo.getSex().getValue() == 1 ? "man" : "women");
        userInfoVo.setMarriage(StrUtil.equals("已婚", userInfo.getMarriage()) ? 1 : 0);
        return userInfoVo;
    }

    public Boolean updateUserInfo(UserInfoVo userInfoVo) {
        User user = UserThreadLocal.get();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setAge(Integer.valueOf(userInfoVo.getAge()));
        userInfo.setSex(StringUtils.equalsIgnoreCase(userInfoVo.getGender(), "man") ? SexEnum.MAN : SexEnum.WOMAN);
        userInfo.setBirthday(userInfoVo.getBirthday());
        userInfo.setCity(userInfoVo.getCity());
        userInfo.setEdu(userInfoVo.getEducation());
        userInfo.setIncome(StringUtils.replaceAll(userInfoVo.getIncome(), "K", ""));
        userInfo.setIndustry(userInfoVo.getProfession());
        userInfo.setMarriage(userInfoVo.getMarriage() == 1 ? "已婚" : "未婚");
        return this.userInfoService.updateUserInfoByUserId(userInfo);
    }

    public CountsVo queryCounts() {
        CountsVo countsVo = new CountsVo();
        Long userId = UserThreadLocal.get().getId();
        countsVo.setEachLoveCount(userLikeApi.queryMutualLikeCount(userId));
        countsVo.setFanCount(userLikeApi.queryFanCount(userId));
        countsVo.setLoveCount(userLikeApi.queryLikeCount(userId));
        return countsVo;
    }

    public PageResult queryLikeList(Integer type, Integer page, Integer pageSize, String nickname) {
        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        Long userId = UserThreadLocal.get().getId();
        List<Object> userIdList = null;

        //1 互相关注 2 我关注 3 粉丝 4 谁看过我
        switch (type) {
            case 1: {
                PageInfo<UserLike> pageInfo = userLikeApi.queryMutualLikeList(userId, page, pageSize);
                userIdList = CollUtil.getFieldValues(pageInfo.getRecords(), "userId");
                break;
            }
            case 2: {
                PageInfo<UserLike> pageInfo = userLikeApi.queryLikeList(userId, page, pageSize);
                userIdList = CollUtil.getFieldValues(pageInfo.getRecords(), "likeUserId");
                break;
            }
            case 3: {
                PageInfo<UserLike> pageInfo = userLikeApi.queryFanList(userId, page, pageSize);
                userIdList = CollUtil.getFieldValues(pageInfo.getRecords(), "userId");
                break;
            }
            case 4: {
                PageInfo<Visitors> pageInfo = visitorsApi.topVisitor(userId, page, pageSize);
                userIdList = CollUtil.getFieldValues(pageInfo.getRecords(), "visitorUserId");
                break;
            }
            default:
                return pageResult;
        }

        if (CollUtil.isEmpty(userIdList)) {
            return pageResult;
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIdList);
        if (StrUtil.isNotEmpty(nickname)) {
            queryWrapper.like("nike_name", nickname);
        }
        List<UserInfo> userInfoList = userInfoService.queryUserInfoList(queryWrapper);
        List<UserLikeListVo> userLikeListVos = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            UserLikeListVo userLikeListVo = new UserLikeListVo();
            userLikeListVo.setAge(userInfo.getAge());
            userLikeListVo.setAvatar(userInfo.getLogo());
            userLikeListVo.setCity(userInfo.getCity());
            userLikeListVo.setEducation(userInfo.getEdu());
            userLikeListVo.setGender(userInfo.getSex().name().toLowerCase());
            userLikeListVo.setId(userInfo.getUserId());
            userLikeListVo.setMarriage(StringUtils.equals(userInfo.getMarriage(), "已婚") ? 1 : 0);
            userLikeListVo.setNickname(userInfo.getNickName());
            //是否喜欢  userLikeApi中的isLike开放出来
            userLikeListVo.setAlreadyLove(this.userLikeApi.isLike(userId, userInfo.getUserId()));


            Double score = this.recommendUserService.queryScore(userId, userInfo.getUserId());
            userLikeListVo.setMatchRate(Convert.toInt(score));

            userLikeListVos.add(userLikeListVo);
        }

        pageResult.setItems(userLikeListVos);
        return pageResult;
    }

    /**
     * 取消喜欢
     *
     * @param userId
     */
    public void disLike(Long userId) {
        //判断当前用户与此用户是否相互喜欢
        User user = UserThreadLocal.get();
        Boolean mutualLike = userLikeApi.isMutualLike(user.getId(), userId);

        //取消喜欢
        userLikeApi.notLikeUser(user.getId(), userId);

        if (mutualLike) {
            //取消好友关系,解除在环信平台的好友关系
            imService.removeUser(userId);
        }
    }

    /**
     * 喜欢
     *
     * @param userId
     */
    public void likeFan(Long userId) {
        //喜欢用户,如果用户是相互喜欢的话就会成为好友
        tanHuaService.likeUser(userId);
    }

    public SettingsVo querySettings() {
        SettingsVo settingsVo = new SettingsVo();
        User user = UserThreadLocal.get();

        //设置用户基本信息
        settingsVo.setId(user.getId());
        settingsVo.setPhone(user.getMobile());

        //查询用户配置信息
        Settings settings = settingsService.querySettings(user.getId());
        if (ObjectUtil.isNotEmpty(settings)) {
            settingsVo.setGonggaoNotification(settings.getGonggaoNotification());
            settingsVo.setLikeNotification(settings.getLikeNotification());
            settingsVo.setPinglunNotification(settings.getPinglunNotification());
        }

        //查询陌生人问题
        settingsVo.setStrangerQuestion(tanHuaService.queryQuestion(user.getId()));
        return settingsVo;
    }

    public void saveQuestions(String content) {
        User user = UserThreadLocal.get();
        questionService.saveQuestions(user.getId(), content);
    }

    public PageResult queryBlacklist(Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();

        IPage<BlackList> blackListIPage = this.blackListService.queryBlacklist(user.getId(), page, pageSize);

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);
        pageResult.setCounts(Convert.toInt(blackListIPage.getTotal()));
        pageResult.setPages(Convert.toInt(blackListIPage.getPages()));

        List<BlackList> records = blackListIPage.getRecords();
        if(CollUtil.isEmpty(records)){
            return pageResult;
        }

        List<Object> userIds = CollUtil.getFieldValues(records, "blackUserId");
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoByUserIdList(userIds);

        List<BlackListVo> blackListVos = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            BlackListVo blackListVo = new BlackListVo();
            blackListVo.setAge(userInfo.getAge());
            blackListVo.setAvatar(userInfo.getLogo());
            blackListVo.setGender(userInfo.getSex().name().toLowerCase());
            blackListVo.setId(userInfo.getUserId());
            blackListVo.setNickname(userInfo.getNickName());

            blackListVos.add(blackListVo);
        }

        pageResult.setItems(blackListVos);

        return pageResult;
    }

    public void delBlacklist(Long userId) {
        User user = UserThreadLocal.get();
        blackListService.delBlacklist(user.getId(),userId);
    }

    public void updateNotification(Boolean likeNotification, Boolean pinglunNotification, Boolean gonggaoNotification) {
        User user = UserThreadLocal.get();
        settingsService.updateNotification(user.getId(),likeNotification,pinglunNotification,gonggaoNotification);
    }
}