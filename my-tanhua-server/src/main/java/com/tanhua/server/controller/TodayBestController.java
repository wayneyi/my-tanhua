package com.tanhua.server.controller;

import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.utils.Cache;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("tanhua")
@Slf4j
public class TodayBestController {
    @Autowired
    private TodayBestService todayBestService;

    /**
     * 查询今日佳人
     *
     * @param token
     * @return
     */
    @GetMapping("todayBest")
    public ResponseEntity<TodayBest> queryTodayBest(@RequestHeader("Authorization") String token) {
        try {
            TodayBest todayBest = todayBestService.queryTodayBest(token);
            if (null != todayBest) {
                return ResponseEntity.ok(todayBest);
            }
        } catch (Exception e) {
            log.error("查询今日佳人出错~ token = " + token, e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    /**
     * 查询推荐用户列表
     * @param token
     * @param queryParam
     * @return
     */
    @GetMapping("recommendation")
    @Cache
    public ResponseEntity<PageResult> queryRecommendation(@RequestHeader("Authorization") String token,
                                                          RecommendUserQueryParam queryParam){
        try {
            PageResult pageResult = todayBestService.queryRecommendation(token,queryParam);
            if (null != pageResult) {
                return ResponseEntity.ok(pageResult);
            }
        } catch (Exception e) {
            log.error("查询推荐用户列表出错~ token = " + token, e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
