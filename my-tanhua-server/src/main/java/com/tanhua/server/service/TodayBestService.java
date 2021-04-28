package com.tanhua.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class TodayBestService {
    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Value("${tanhua.sso.default.user}")
    private Long defaultUser;

    public TodayBest queryTodayBest(String token) {
        //校验token是否有效,通过sso接口进行校验
        User user = userService.queryUserByToken(token);
        if (null == user) {
            //token非法或已过期
            return null;
        }
        //查询推荐用户(今日佳人)
        TodayBest todayBest = recommendUserService.queryTodayBest(user.getId());
        if (null == todayBest) {
            //给出默认的推荐
            todayBest = new TodayBest();
            todayBest.setId(defaultUser);
            todayBest.setFateValue(80L);    //固定值
        }

        //补全个人信息
        UserInfo userInfo = userInfoService.queryUserInfoByUserId(todayBest.getId());
        if (null == userInfo) {
            return null;
        }
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
        todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
        todayBest.setAge(userInfo.getAge());

        return todayBest;
    }

    public PageResult queryRecommendation(String token, RecommendUserQueryParam queryParam) {
        //校验token是否有效,通过sso接口进行校验
        User user = userService.queryUserByToken(token);
        if (null == user) {
            //token非法或已过期
            return null;
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(queryParam.getPage());
        pageResult.setPages(queryParam.getPagesize());

        PageInfo<RecommendUser> pageInfo = recommendUserService.queryRecommendUserList(user.getId(), queryParam.getPage(), queryParam.getPagesize());
        List<RecommendUser> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            //没有查询到推荐的用户列表
            return pageResult;
        }

        //填充个人信息

        //收集推荐用户的id
        Set<Long> userIds = new HashSet<>();
        for (RecommendUser record : records) {
            userIds.add(record.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        //用户id参数
        queryWrapper.in("user_id", userIds);

        if (StringUtils.isNotEmpty(queryParam.getGender())) {
            //需要性别参数查询
//            queryWrapper.eq("sex", StringUtils.equals(queryParam.getGender(), "man") ? 1 : 2);
        }

        if(StringUtils.isNotEmpty(queryParam.getCity())){
            //需要城市参数查询
//            queryWrapper.like("city",queryParam.getCity());
        }

        if(queryParam.getAge() != null){
            //设置年龄参数,条件: 小于等于
//            queryWrapper.le("age",queryParam.getAge());
        }

        List<UserInfo> userInfoList = userInfoService.queryUserInfoList(queryWrapper);
        if(CollectionUtils.isEmpty(userInfoList)){
            //没有查询到用户基本信息
            return pageResult;
        }

        //补全个人信息
        List<TodayBest> todayBests = new ArrayList<>();
        for(UserInfo userInfo : userInfoList){
            TodayBest todayBest = new TodayBest();

            todayBest.setId(userInfo.getUserId());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
            todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
            todayBest.setAge(userInfo.getAge());

            //缘分值
            for (RecommendUser record : records) {
                if(record.getUserId().longValue() == userInfo.getUserId().longValue()){
                    double score = Math.floor(record.getScore());//取整,98.2 -> 98
                    todayBest.setFateValue(Double.valueOf(score).longValue());
                    break;
                }
            }

            todayBests.add(todayBest);
        }

        //按缘分值进行倒序排序
        Collections.sort(todayBests,(o1,o2)->new Long(o2.getFateValue() - o1.getFateValue()).intValue());
        pageResult.setItems(todayBests);
        return pageResult;
    }
}
