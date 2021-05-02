package com.tanhua.server.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.common.pojo.Question;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.api.HuanXinApi;
import com.tanhua.dubbo.server.api.VisitorsApi;
import com.tanhua.dubbo.server.enums.HuanXinMessageType;
import com.tanhua.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TanHuaService {
    @Reference(version = "1.0.0")
    private HuanXinApi huanXinApi;

    @Reference(version = "1.0.0")
    private VisitorsApi visitorsApi;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private QuestionService questionService;

    public TodayBest queryUserInfo(Long userId) {
        UserInfo userInfo = userInfoService.queryUserInfoByUserId(userId);
        if(ObjectUtil.isEmpty(userInfo)){
            return null;
        }

        TodayBest todayBest = new TodayBest();
        todayBest.setId(userId);
        todayBest.setAge(userInfo.getAge());
        todayBest.setGender(userInfo.getSex().name().toLowerCase());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(Convert.toStrArray(StrUtil.split(userInfo.getTags(),',')));
        todayBest.setAvatar(userInfo.getLogo());
        //缘分值
        User user = UserThreadLocal.get();
        todayBest.setFateValue(recommendUserService.queryScore(userId,user.getId()).longValue());

        //记录来访用户
        visitorsApi.saveVisitor(userId,user.getId(),"个人主页");

        return todayBest;
    }

    public String queryQuestion(Long userId) {
        Question question = questionService.queryQuestion(userId);
        if(ObjectUtil.isNotEmpty(question)){
            return question.getTxt();
        }
        //默认问题
        return "你的爱好是什么?";
    }

    public Boolean replyQuestion(Long userId, String reply) {
        User user = UserThreadLocal.get();
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(user.getId());

        //构建消息内容
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", user.getId());
        msg.put("huanXinId", "HX_" + user.getId());
        msg.put("nickname", userInfo.getNickName());
        msg.put("strangerQuestion", this.queryQuestion(userId));
        msg.put("reply", reply);

        //发送环信消息
        return this.huanXinApi.sendMsgFromAdmin("HX_" + userId,
                HuanXinMessageType.TXT, JSONUtil.toJsonStr(msg));
    }
}
