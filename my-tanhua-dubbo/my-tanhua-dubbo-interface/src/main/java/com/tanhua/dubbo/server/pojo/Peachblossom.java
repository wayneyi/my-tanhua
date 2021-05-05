package com.tanhua.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "peachblossom")
public class Peachblossom implements java.io.Serializable {
    private static final long serialVersionUID = -313234236884933873L;

    private ObjectId id; //主键id
    private Long vid; //自增长id
    private Long userId;
    private String voiceUrl; //视频文件
    private Long created; //创建时间
    private Integer seeType; // 谁可以看，1-公开，2-私密，3-部分可见，4-不给谁看
//    private List<Long> seeList; //部分可见的列表
//    private List<Long> notSeeList; //不给谁看的列表
//    private String longitude; //经度
//    private String latitude; //纬度
//    private String locationName; //位置名称
}