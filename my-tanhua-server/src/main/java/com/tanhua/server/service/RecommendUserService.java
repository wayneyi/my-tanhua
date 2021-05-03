package com.tanhua.server.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.TodayBest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 负责与dubbo服务进行交互
 */
@Service
public class RecommendUserService {
    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

    public TodayBest queryTodayBest(Long userId) {
        RecommendUser recommendUser = recommendUserApi.queryWithMaxScore(userId);
        if(null == recommendUser){
            return null;
        }

        TodayBest todayBest = new TodayBest();
        todayBest.setId(recommendUser.getUserId());
        //缘分值
        double score = Math.floor(recommendUser.getScore());//取整,98.2 -> 98
        todayBest.setFateValue(Double.valueOf(score).longValue());
        return todayBest;
    }

    public PageInfo<RecommendUser> queryRecommendUserList(Long userId, Integer page, Integer pageSize) {
        return recommendUserApi.queryPageInfo(userId,page,pageSize);
    }

    /**
     * 查询探花卡片列表
     * @param userId
     * @param count
     * @return
     */
    public List<RecommendUser> queryCardList(Long userId, Integer count) {
        return recommendUserApi.queryCardList(userId,count);
    }

    public Double queryScore(Long userId,Long toUserId){
        Double score = recommendUserApi.queryScore(userId, toUserId);
        if(ObjectUtil.isNotEmpty(score)){
            return score;
        }
        //默认值
        return 98d;
    }
}
