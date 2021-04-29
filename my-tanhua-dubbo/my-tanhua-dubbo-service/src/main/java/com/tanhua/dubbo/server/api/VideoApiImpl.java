package com.tanhua.dubbo.server.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.enums.IdType;
import com.tanhua.dubbo.server.pojo.Video;
import com.tanhua.dubbo.server.service.IdService;
import com.tanhua.dubbo.server.vo.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0")
@Slf4j
public class VideoApiImpl implements VideoApi {
    @Autowired
    private IdService idService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 发布小视屏
     * @param video
     * @return
     */
    @Override
    public String saveVideo(Video video) {
        try {
            //校验
            if(!ObjectUtil.isAllNotEmpty(video.getUserId(),video.getPicUrl(),video.getVideoUrl())){
                return null;
            }

            //设置id
            video.setId(ObjectId.get());
            video.setVid(idService.createId(IdType.VIDEO));

            //发布时间
            video.setCreated(System.currentTimeMillis());

            //保存到Mongodb中
            mongoTemplate.save(video);

            return video.getId().toHexString();
        } catch (Exception e) {
            log.error("小视屏发布失败~ video = " + video, e);
        }
        return null;
    }

    /**
     * 查询小视屏列表,优先展现推荐的视屏,如果没有推荐的视屏或已经查询完成,就需要查询系统视屏数据
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<Video> queryVideoList(Long userId, Integer page, Integer pageSize) {
        PageInfo<Video> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);

        //从redis中获取推荐视屏数据
//        String redisKey = "QUANZI_VIDEO_RECOMMEND_"+userId;
//        String redisData = redisTemplate.opsForValue().get(redisKey);

        List<Long> vids = new ArrayList<>();
        int recommendCount = 0;
//        if(StrUtil.isNotEmpty(redisData)){
//            //手动分页查询数据
//            List<String> vidList = StrUtil.split(redisData, ',');
//            //计算分页
//            int[] startEnd = PageUtil.transToStartEnd(page - 1, pageSize);//[0,10]
//            int startIndex = startEnd[0];   //开始
//            int endIndex = Math.min(startEnd[1], vidList.size());    //结束
//
//            for (int i = startIndex; i < endIndex; i++) {
//                vids.add(Convert.toLong(vidList.get(i)));
//            }
//            recommendCount = vidList.size();
//        }

        if(CollUtil.isEmpty(vids)){
            //没有推荐或前面推荐已经查询完毕,查询系统的视屏数据

            //计算前面的推荐视屏页数
            int totalPage = PageUtil.totalPage(recommendCount, pageSize);

            PageRequest pageRequest = PageRequest.of(page - totalPage -1,pageSize,
                    Sort.by(Sort.Order.desc("created")));
            Query query = new Query().with(pageRequest);
            List<Video> videoList = mongoTemplate.find(query, Video.class);
            pageInfo.setRecords(videoList);

            return pageInfo;
        }

        //根据vid查询对应的视屏数据
        Query query = Query.query(Criteria.where("vid").in(vids));
        List<Video> videoList = mongoTemplate.find(query, Video.class);
        pageInfo.setRecords(videoList);

        return pageInfo;
    }
}
