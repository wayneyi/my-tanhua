package com.tanhua.server.controller;

import cn.hutool.core.util.StrUtil;
import com.tanhua.server.service.QuanZiService;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.VisitorsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("movements")
public class QuanZiController {
    @Autowired
    private QuanZiService quanZiService;

    /**
     * 查询好友动态
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping
    public PageResult queryPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        return this.quanZiService.queryPublishList(page, pageSize);
    }

    //com.tanhua.server.controller.QuanZiController
    /**
     * 发送动态
     *
     * @param textContent
     * @param location
     * @param multipartFile
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> savePublish(@RequestParam("textContent") String textContent,
                                            @RequestParam(value = "location", required = false) String location,
                                            @RequestParam(value = "latitude", required = false) String latitude,
                                            @RequestParam(value = "longitude", required = false) String longitude,
                                            @RequestParam(value = "imageContent", required = false) MultipartFile[] multipartFile) {
        try {
            String publishId = this.quanZiService.savePublish(textContent, location, latitude, longitude, multipartFile);
            if (StrUtil.isNotEmpty(publishId)) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询推荐动态
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("recommend")
    public PageResult queryRecommendPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        return this.quanZiService.queryRecommendPublishList(page, pageSize);
    }

    /**
     * 自己的所有动态
     *
     * @return
     */
    @GetMapping("all")
    public ResponseEntity<PageResult> queryAlbumList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                     @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize,
                                                     @RequestParam(value = "userId") Long userId) {
        try {
            PageResult pageResult = this.quanZiService.queryAlbumList(userId, page, pageSize);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 谁看过我
     *
     * @return
     */
    @GetMapping("visitors")
    public ResponseEntity<List<VisitorsVo>> queryVisitorsList(){
        try {
            List<VisitorsVo> list = this.quanZiService.queryVisitorsList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
