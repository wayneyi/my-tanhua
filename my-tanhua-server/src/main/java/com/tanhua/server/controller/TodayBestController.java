package com.tanhua.server.controller;

import com.tanhua.common.utils.Cache;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.server.service.TodayBestService;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
     * @return
     */
    @GetMapping("todayBest")
    public ResponseEntity<TodayBest> queryTodayBest() {
        try {
            TodayBest todayBest = todayBestService.queryTodayBest();
            if (null != todayBest) {
                return ResponseEntity.ok(todayBest);
            }
        } catch (Exception e) {
            log.error("查询今日佳人出错~ userId = " + UserThreadLocal.get().getId(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    /**
     * 查询推荐用户列表
     * @param queryParam
     * @return
     */
    @GetMapping("recommendation")
    @Cache
    public ResponseEntity<PageResult> queryRecommendation(RecommendUserQueryParam queryParam){
        try {
            PageResult pageResult = todayBestService.queryRecommendation(queryParam);
            if (null != pageResult) {
                return ResponseEntity.ok(pageResult);
            }
        } catch (Exception e) {
            log.error("查询推荐用户列表出错~ userId = " + UserThreadLocal.get().getId(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
