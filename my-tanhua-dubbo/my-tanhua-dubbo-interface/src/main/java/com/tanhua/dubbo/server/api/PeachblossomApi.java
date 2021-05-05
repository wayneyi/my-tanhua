package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.pojo.Peachblossom;
import com.tanhua.dubbo.server.vo.PeachblossomVo;

public interface PeachblossomApi {
    /**
     * 保存语音
     *
     * @param voice
     * @return 保存成功后，返回视频id
     */
    String saveVoice(Peachblossom voice);

    PeachblossomVo randomVoice();
}
