package com.tanhua.dubbo.server.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.UserLike;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service(version = "1.0.0")
public class UserLikeApiImpl implements UserLikeApi {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public static final String LIKE_REDIS_KEY_PREFIX = "USER_LIKE_";

    public static final String NOT_LIKE_REDIS_KEY_PREFIX = "USER_NOT_LIKE_";

    /**
     * 喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    @Override
    public Boolean likeUser(Long userId, Long likeUserId) {
        //判断用户是否已经喜欢,如果已经喜欢就返回
        if(isLike(userId,likeUserId)){
            return false;
        }

        UserLike userLike = new UserLike();
        userLike.setId(ObjectId.get());
        userLike.setUserId(userId);
        userLike.setLikeUserId(likeUserId);
        userLike.setCreated(System.currentTimeMillis());

        //将数据存储到MongoDB
        mongoTemplate.save(userLike);

        //用户的喜欢数据保存到redis
        //用户1: key -> USER_LIKE_1, value -> 2,"1"
        //用户1: key -> USER_LIKE_1, value -> 3,"1"
        //用户2: key -> USER_LIKE_2, value -> 4,"1"
        String redisKey = getLikeRedisKey(userId);
        String hashKey = String.valueOf(likeUserId);
        redisTemplate.opsForHash().put(redisKey,hashKey,"1");

        //判断,喜欢的用户是否在不喜欢的列表中,如果在,就需要删除
        if(isNotLike(userId,likeUserId)){
            redisKey = getNotLikeRedisKey(userId);
            redisTemplate.opsForHash().delete(redisKey,hashKey);
        }
        return true;
    }

    /**
     * 获取喜欢数据的redis key
     * @param userId
     * @return
     */
    private String getLikeRedisKey(Long userId){
        return LIKE_REDIS_KEY_PREFIX + userId;
    }

    /**
     * 获取不喜欢数据的redis key
     * @param userId
     * @return
     */
    private String getNotLikeRedisKey(Long userId){
        return NOT_LIKE_REDIS_KEY_PREFIX + userId;
    }

    /**
     * 是否喜欢
     * @param userId
     * @param likeUserId
     * @return
     */
    private Boolean isLike(Long userId, Long likeUserId){
        String redisKey = getLikeRedisKey(userId);
        String hashKey = String.valueOf(likeUserId);
        return redisTemplate.opsForHash().hasKey(redisKey,hashKey);
    }

    /**
     * 是否不喜欢
     * @param userId
     * @param likeUserId
     * @return
     */
    private Boolean isNotLike(Long userId, Long likeUserId){
        String redisKey = getNotLikeRedisKey(userId);
        String hashKey = String.valueOf(likeUserId);
        return redisTemplate.opsForHash().hasKey(redisKey,hashKey);
    }

    @Override
    public Boolean notLikeUser(Long userId, Long likeUserId) {
        //判断用户是否已经不喜欢,如果已经不喜欢,就返回
        if(isNotLike(userId,likeUserId)){
            return false;
        }

        //将用户保存到不喜欢列表
        String redisKey = getNotLikeRedisKey(userId);
        String hashKey = String.valueOf(likeUserId);
        redisTemplate.opsForHash().put(redisKey,hashKey,"1");

        //判断用户是否在喜欢列表中,如果存在的话,需要删除数据
        if(isLike(userId,likeUserId)){
            //删除mongoDB数据
            Query query = Query.query(Criteria.where("userId").is(userId)
                    .and("likeUserId").is(likeUserId));
            mongoTemplate.remove(query,UserLike.class);

            //删除redis中数据
            redisKey = getLikeRedisKey(userId);
            redisTemplate.opsForHash().delete(redisKey,hashKey);
        }
        return true;
    }

    @Override
    public Boolean isMutualLike(Long userId, Long likeUserId) {
        return isLike(userId,likeUserId) && isLike(likeUserId,userId);
    }

    @Override
    public List<Long> queryLikeList(Long userId) {
        //查询redis
        String redisKey = getLikeRedisKey(userId);
        Set<Object> keys = redisTemplate.opsForHash().keys(redisKey);
        if(CollUtil.isEmpty(keys)){
            return ListUtil.empty();
        }

        List<Long> result = new ArrayList<>(keys.size());
        keys.forEach(o -> result.add(Convert.toLong(o)));
        return result;
    }

    @Override
    public List<Long> queryNotLikeList(Long userId) {
        //查询redis
        String redisKey = getNotLikeRedisKey(userId);
        Set<Object> keys = redisTemplate.opsForHash().keys(redisKey);
        if(CollUtil.isEmpty(keys)){
            return ListUtil.empty();
        }

        List<Long> result = new ArrayList<>(keys.size());
        keys.forEach(o -> result.add(Convert.toLong(o)));
        return result;
    }
}
