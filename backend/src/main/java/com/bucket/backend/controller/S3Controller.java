package com.bucket.backend.controller;


import com.bucket.backend.service.S3Service;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class S3Controller {

    private static final Logger log = LoggerFactory.getLogger(S3Controller.class);
    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        // 서비스에서 S3 업로드 실행
        log.info("파일 저장 컨트롤러 실행");
        try{
            String filekey = s3Service.uploadFile(file,"videos");
            return ResponseEntity.ok(filekey+" 파일을 업로드 했습니다.");
        } catch (Exception e) {
            log.error(" 파일 업로드 실패", e);
            return ResponseEntity.status(400).build();
        }
    }
}
