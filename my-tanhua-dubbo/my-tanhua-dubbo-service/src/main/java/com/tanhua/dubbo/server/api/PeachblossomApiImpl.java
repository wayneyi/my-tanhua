package com.tanhua.dubbo.server.api;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.enums.IdType;
import com.tanhua.dubbo.server.pojo.Peachblossom;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.service.IdService;
import com.tanhua.dubbo.server.vo.PeachblossomVo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

@Service(version = "1.0.0")
@Slf4j
public class PeachblossomApiImpl implements PeachblossomApi {
    @Autowired
    private IdService idService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public String saveVoice(Peachblossom voice) {
        try {
            //校验
            if (!ObjectUtil.isAllNotEmpty(voice.getUserId(), voice.getVoiceUrl())) {
                return null;
            }

            //设置id
            voice.setId(ObjectId.get());
            voice.setVid(idService.createId(IdType.VOICE));

            //发布时间
            voice.setCreated(System.currentTimeMillis());

            //保存到Mongodb中
            mongoTemplate.save(voice);

            return voice.getId().toHexString();
        } catch (Exception e) {
            log.error("小视屏发布失败~ voice = " + voice, e);
        }
        return null;
    }

    @Override
    public PeachblossomVo randomVoice() {
        //获取语音数量
        String redisValue = redisTemplate.opsForValue().get("TANHUA_ID_VOICE");
        int index = Integer.parseInt(redisValue);

        //随机查询一个语音
        int vid = RandomUtil.randomInt(1, index + 1);

        //根据vid查询用户信息
        Query query = Query.query(Criteria.where("vid").is(vid));
        Peachblossom peachblossom = mongoTemplate.findOne(query, Peachblossom.class);

        //封装PeachblossomVo对象
        PeachblossomVo peachblossomVo = new PeachblossomVo();
        String s = String.valueOf(peachblossom.getUserId());
        peachblossomVo.setId(Integer.parseInt(s));
        peachblossomVo.setSoundUrl(peachblossom.getVoiceUrl());

        return peachblossomVo;
    }
}
