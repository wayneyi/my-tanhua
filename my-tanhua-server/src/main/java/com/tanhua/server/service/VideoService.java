package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.service.PicUploadService;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.common.vo.PicUploadResult;
import com.tanhua.dubbo.server.api.VideoApi;
import com.tanhua.dubbo.server.pojo.Video;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.VideoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VideoService {
    @Reference(version = "1.0.0")
    private VideoApi videoApi;

    @Autowired
    private PicUploadService picUploadService;


    @Autowired
    protected FastFileStorageClient storageClient;

    @Autowired
    private FdfsWebServer fdfsWebServer;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 小视屏上传
     *
     * @param picFile   封面图片
     * @param videoFile 视屏文件
     * @return
     */
    public Boolean saveVideo(MultipartFile picFile, MultipartFile videoFile) {
        User user = UserThreadLocal.get();

        Video video = new Video();
        video.setUserId(user.getId());
        video.setSeeType(1);

        try {
            //上传图片到阿里云oss
            PicUploadResult uploadResult = picUploadService.upload(picFile);
            video.setPicUrl(uploadResult.getName());

            //上传视屏到FastDFS中
            StorePath storePath = this.storageClient.uploadFile(videoFile.getInputStream(),
                    videoFile.getSize(),
                    // aa-bb.cc.mp4
                    StrUtil.subAfter(videoFile.getOriginalFilename(), '.', true),
                    null
            );
            video.setVideoUrl(fdfsWebServer.getWebServerUrl() + storePath.getFullPath());

            String videoId = videoApi.saveVideo(video);
            return StrUtil.isNotEmpty(videoId);
        } catch (IOException e) {
            log.error("上传小视屏出错~ userId = " + user.getId() + ",file = " +
                    videoFile.getOriginalFilename(), e);
        }
        return null;
    }

    public PageResult queryVideoList(Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        PageInfo<Video> pageInfo = this.videoApi.queryVideoList(user.getId(), page, pageSize);
        List<Video> records = pageInfo.getRecords();

        if(CollUtil.isEmpty(records)){
            return pageResult;
        }

        //查询用户信息
        List<Object> userIds = CollUtil.getFieldValues(records, "userId");
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoByUserIdList(userIds);

        List<VideoVo> videoVoList = new ArrayList<>();
        for (Video record : records) {
            VideoVo videoVo = new VideoVo();

            videoVo.setUserId(record.getUserId());
            videoVo.setCover(record.getPicUrl());
            videoVo.setVideoUrl(record.getVideoUrl());
            videoVo.setId(record.getId().toHexString());
            videoVo.setSignature("我就是我~"); //TODO 签名

            videoVo.setCommentCount(0); //TODO 评论数
            videoVo.setHasFocus(0); //TODO 是否关注
            videoVo.setHasLiked(0); //TODO 是否点赞（1是，0否）
            videoVo.setLikeCount(0);//TODO 点赞数

            //填充用户信息
            for (UserInfo userInfo : userInfoList) {
                if (ObjectUtil.equals(videoVo.getUserId(), userInfo.getUserId())) {
                    videoVo.setNickname(userInfo.getNickName());
                    videoVo.setAvatar(userInfo.getLogo());
                    break;
                }
            }

            videoVoList.add(videoVo);
        }

        pageResult.setItems(videoVoList);
        return pageResult;
    }
}
