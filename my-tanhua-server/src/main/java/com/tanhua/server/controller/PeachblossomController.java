package com.tanhua.server.controller;

import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.vo.PeachblossomVo;
import com.tanhua.server.service.PeachblossomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("peachblossom")
@Slf4j
public class PeachblossomController {
    @Autowired
    private PeachblossomService peachblossomService;

    /**
     * 发送
     */
    @PostMapping
    public ResponseEntity<Void> sendVoice(@RequestParam("soundFile") MultipartFile voice){
        try {
            Boolean bool = peachblossomService.sendVoice(voice);
            if(bool){
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            log.error("发送语音出错~ userId = " + UserThreadLocal.get().getId(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping
    public ResponseEntity<PeachblossomVo> receiveVoice(){
        try {
            PeachblossomVo peachblossomVo = peachblossomService.receiveVoice();
            if (null != peachblossomVo) {
                return ResponseEntity.ok(peachblossomVo);
            }
        } catch (Exception e) {
            log.error("接收语音出错~ userId = " + UserThreadLocal.get().getId(), e);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
