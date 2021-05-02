package com.tanhua.server.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.service.PicUploadService;
import com.tanhua.common.utils.RelativeDateFormat;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.common.vo.PicUploadResult;
import com.tanhua.dubbo.server.api.QuanZiApi;
import com.tanhua.dubbo.server.api.VisitorsApi;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.pojo.Visitors;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.QuanZiVo;
import com.tanhua.server.vo.VisitorsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class QuanZiService {
    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Reference(version = "1.0.0")
    private VisitorsApi visitorsApi;

    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PicUploadService picUploadService;

    public PageResult queryPublishList(Integer page, Integer pageSize) {
        //分析:通过dubbo中的服务查询用户的好友动态
        //通过mysql查询用户的信息,回写到结果对象中(QuanZiVo)
        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        //校验token是否有效
//        User user = userService.queryUserByToken(token);
//        if(user == null){
//            //token已经失效
//            return pageResult;
//        }

        //直接从ThreadLocal中获取对象
        User user = UserThreadLocal.get();

        //通过dubbo查询数据
        PageInfo<Publish> pageInfo = quanZiApi.queryPublishList(user.getId(), page, pageSize);
        List<Publish> records = pageInfo.getRecords();
        if(CollUtil.isEmpty(records)){
            return pageResult;
        }

        pageResult.setItems(fillQuanZiVo(records));
        return pageResult;
    }

    /**
     * 填充用户信息
     * @param userInfo
     * @param quanZiVo
     */
    private void fillUserInfoToQuanZiVo(UserInfo userInfo,QuanZiVo quanZiVo){
        BeanUtil.copyProperties(userInfo, quanZiVo, "id");
        quanZiVo.setGender(userInfo.getSex().name().toLowerCase());
        quanZiVo.setTags(StringUtils.split(userInfo.getTags(), ','));

        quanZiVo.setCommentCount(0); //TODO 评论数
        quanZiVo.setDistance("1.2公里"); //TODO 距离
        quanZiVo.setHasLiked(0); //TODO 是否点赞（1是，0否）
        quanZiVo.setLikeCount(0); //TODO 点赞数
        quanZiVo.setHasLoved(0); //TODO 是否喜欢（1是，0否）
        quanZiVo.setLoveCount(0); //TODO 喜欢数
    }

    /**
     * 根据查询到的publish集合填充QuanZiVo对象
     * @param records
     * @return
     */
    private List<QuanZiVo> fillQuanZiVo(List<Publish> records){
        List<QuanZiVo> quanZiVoList = new ArrayList<>();
        records.forEach(publish -> {
            QuanZiVo quanZiVo = new QuanZiVo();
            quanZiVo.setId(publish.getId().toHexString());
            quanZiVo.setTextContent(publish.getText());
            quanZiVo.setImageContent(publish.getMedias().toArray(new String[]{}));
            quanZiVo.setUserId(publish.getUserId());
            quanZiVo.setCreateDate(RelativeDateFormat.format(new Date(publish.getCreated())));

            quanZiVoList.add(quanZiVo);
        });

        //查询用户信息
        List<Object> userIds = CollUtil.getFieldValues(records, "userId");
        List<UserInfo> userInfoList = userInfoService.queryUserInfoByUserIdList(userIds);
        for (QuanZiVo quanZiVo : quanZiVoList) {
            //找到对应的用户信息
            for (UserInfo userInfo : userInfoList) {
                if(quanZiVo.getUserId().longValue() == userInfo.getUserId().longValue()){
                    fillUserInfoToQuanZiVo(userInfo,quanZiVo);
                    break;
                }
            }
        }
        return quanZiVoList;
    }


    /**
     * 发布动态
     * @param textContent
     * @param location
     * @param latitude
     * @param longitude
     * @param multipartFile
     * @return
     */
    public String savePublish(String textContent,
                              String location,
                              String latitude,
                              String longitude,
                              MultipartFile[] multipartFile) {
        //查询当前的登录信息
        User user = UserThreadLocal.get();

        Publish publish = new Publish();
        publish.setUserId(user.getId());
        publish.setText(textContent);
        publish.setLocationName(location);
        publish.setLatitude(latitude);
        publish.setLongitude(longitude);
        publish.setSeeType(1);

        List<String> picUrls = new ArrayList<>();
        //图片上传
        for (MultipartFile file : multipartFile) {
            PicUploadResult picUploadResult = this.picUploadService.upload(file);
            picUrls.add(picUploadResult.getName());
        }

        publish.setMedias(picUrls);
        return this.quanZiApi.savePublish(publish);
    }

    public PageResult queryRecommendPublishList(Integer page, Integer pageSize) {
        //分析:通过dubbo中的服务查询系统推荐动态
        //通过mysql查询用户的信息,回写到结果对象中(QuanZiVo)
        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        //校验token是否有效
//        User user = userService.queryUserByToken(token);
//        if(user == null){
//            //token已经失效
//            return pageResult;
//        }

        //直接从ThreadLocal中获取对象
        User user = UserThreadLocal.get();

        //通过dubbo查询数据
        PageInfo<Publish> pageInfo = quanZiApi.queryRecommendPublishList(user.getId(), page, pageSize);
        List<Publish> records = pageInfo.getRecords();
        if(CollUtil.isEmpty(records)){
            return pageResult;
        }

        pageResult.setItems(fillQuanZiVo(records));
        return pageResult;
    }

    public PageResult queryAlbumList(Long userId, Integer page, Integer pageSize) {
        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        //查询数据
        PageInfo<Publish> pageInfo = quanZiApi.queryAlbumList(userId, page, pageSize);
        if(CollUtil.isEmpty(pageInfo.getRecords())){
            return pageResult;
        }

        //填充数据
        pageResult.setItems(fillQuanZiVo(pageInfo.getRecords()));

        return pageResult;
    }

    public List<VisitorsVo> queryVisitorsList() {
        User user = UserThreadLocal.get();
        List<Visitors> visitorsList = this.visitorsApi.queryMyVisitor(user.getId());
        if (CollUtil.isEmpty(visitorsList)) {
            return Collections.emptyList();
        }

        List<Object> userIds = CollUtil.getFieldValues(visitorsList, "visitorUserId");
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoByUserIdList(userIds);

        List<VisitorsVo> visitorsVoList = new ArrayList<>();

        for (Visitors visitor : visitorsList) {
            for (UserInfo userInfo : userInfoList) {
                if (ObjectUtil.equals(visitor.getVisitorUserId(), userInfo.getUserId())) {

                    VisitorsVo visitorsVo = new VisitorsVo();
                    visitorsVo.setAge(userInfo.getAge());
                    visitorsVo.setAvatar(userInfo.getLogo());
                    visitorsVo.setGender(userInfo.getSex().name().toLowerCase());
                    visitorsVo.setId(userInfo.getUserId());
                    visitorsVo.setNickname(userInfo.getNickName());
                    visitorsVo.setTags(StringUtils.split(userInfo.getTags(), ','));
                    visitorsVo.setFateValue(visitor.getScore().intValue());

                    visitorsVoList.add(visitorsVo);
                    break;
                }
            }
        }

        return visitorsVoList;
    }
}
