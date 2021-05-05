package com.tanhua.server.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.api.PeachblossomApi;
import com.tanhua.dubbo.server.pojo.Peachblossom;
import com.tanhua.dubbo.server.vo.PeachblossomVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class PeachblossomService {
    @Reference(version = "1.0.0")
    private PeachblossomApi peachblossomApi;

    @Autowired
    protected FastFileStorageClient storageClient;

    @Autowired
    private FdfsWebServer fdfsWebServer;

    @Autowired
    private UserInfoService userInfoService;

    public Boolean sendVoice(MultipartFile voice) {
        User user = UserThreadLocal.get();
        Peachblossom peachblossom = new Peachblossom();
        peachblossom.setUserId(user.getId());
        peachblossom.setSeeType(1);

        try {
            StorePath storePath = storageClient.uploadFile(voice.getInputStream(),
                    voice.getSize(),
                    // aa-bb.cc.mp4
                    StrUtil.subAfter(voice.getOriginalFilename(), '.', true),
                    null
            );
            peachblossom.setVoiceUrl(fdfsWebServer.getWebServerUrl() + storePath.getFullPath());

            String voiceId = peachblossomApi.saveVoice(peachblossom);
            return StrUtil.isNotEmpty(voiceId);
        } catch (IOException e) {
            log.error("发送语音出错~ userId = " + user.getId() + ",file = " +
                    voice.getOriginalFilename(), e);
        }
        return null;
    }

    public PeachblossomVo receiveVoice() {
        //无需校验,直接获取
        User user = UserThreadLocal.get();

        try {
            //接收语音
//            PeachblossomVo voice = peachblossomApi.receiveVoice(user.getId());
            PeachblossomVo voice = peachblossomApi.randomVoice();

            //补全个人信息
            UserInfo userInfo = userInfoService.queryUserInfoByUserId(Long.valueOf(voice.getId()));
            voice.setAvatar(userInfo.getLogo());
            voice.setAge(userInfo.getAge());
            voice.setNickname(userInfo.getNickName());
            voice.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");

            //设置今日剩余次数
            voice.setRemainingTimes(10);
            return voice;
        } catch (Exception e) {
            log.error("网络超时,请稍后重试~ ", e);
        }
        return null;
    }
}
