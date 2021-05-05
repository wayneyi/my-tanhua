package com.tanhua.dubbo.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeachblossomVo implements java.io.Serializable {

    private static final long serialVersionUID = 4133419501260037769L;

    private Integer id;
    private String avatar;  //头像
    private String nickname; //昵称
    private String gender; //性别
    private Integer age;
    private String soundUrl; //语音地址
    private Integer remainingTimes; //剩余次数
}
