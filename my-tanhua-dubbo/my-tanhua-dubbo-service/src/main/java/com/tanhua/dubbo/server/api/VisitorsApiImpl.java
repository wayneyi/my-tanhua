package com.tanhua.dubbo.server.api;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.Visitors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Service(version = "1.0.0")
public class VisitorsApiImpl implements VisitorsApi {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String VISITOR_REDIS_KEY = "VISITOR_USER";

    @Override
    public String saveVisitor(Long userId, Long visitorUserId, String from) {
        //校验
        if(!ObjectUtil.isAllNotEmpty(userId,visitorUserId,from)){
            return null;
        }

        //查询访客用户今天是否已经记录过,如果已经记录,则不再记录
        String today = DateUtil.today();
        Long minDate = DateUtil.parseDateTime(today + " 00:00:00").getTime();
        Long maxDate = DateUtil.parseDateTime(today + " 23:59:59").getTime();
        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("visitorUserId").is(visitorUserId)
                .andOperator(Criteria.where("date").gte(minDate),
                        Criteria.where("date").lte(maxDate)
                )
        );
        long count = mongoTemplate.count(query, Visitors.class);
        if(count > 0){
            //今天已经记录过
            return null;
        }

        Visitors visitors = new Visitors();
        visitors.setFrom(from);
        visitors.setVisitorUserId(visitorUserId);
        visitors.setUserId(userId);
        visitors.setDate(System.currentTimeMillis());
        visitors.setId(ObjectId.get());

        //存储数据
        mongoTemplate.save(visitors);

        return visitors.getId().toHexString();
    }

    @Override
    public List<Visitors> queryMyVisitor(Long userId) {
        //查询前5个访客数据,按照访问时间倒序排序
        //如果用户已经查询过列表,记录查询时间,后续查询需要按照这个时间往后查询

        //上一次查询列表的时间
        Long date = Convert.toLong(redisTemplate.opsForHash().get(VISITOR_REDIS_KEY, String.valueOf(userId)));

        PageRequest pageRequest = PageRequest.of(0,5, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageRequest);

        if(ObjectUtil.isNotEmpty(date)){
            query.addCriteria(Criteria.where("date").gte(date));
        }

        List<Visitors> visitorsList = mongoTemplate.find(query, Visitors.class);
        //查询每个来访用户得分
        for (Visitors visitors : visitorsList) {
            Query queryScore = Query.query(Criteria.where("toUserId")
                    .is(userId).and("userId").is(visitors.getVisitorUserId())
            );
            RecommendUser recommendUser = mongoTemplate.findOne(queryScore,RecommendUser.class);
            if(ObjectUtil.isNotEmpty(recommendUser)){
                visitors.setScore(recommendUser.getScore());
            }else{
                //默认得分
                visitors.setScore(90d);
            }
        }
        return visitorsList;
    }
}
