package com.bucket.backend.controller;


import com.bucket.backend.model.UserVideo;
import com.bucket.backend.model.users;
import com.bucket.backend.repository.UserRepository;
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

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class S3Controller {

    private static final Logger log = LoggerFactory.getLogger(S3Controller.class);
    private final S3Service s3Service;
    private final UserRepository userRepository;

    //클라이언트로 받은 파일을 S3에 업로드하고 URL 반환
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("uid") int uid,
                                         @RequestParam("sportname") String sportname,
                                         @RequestParam("feedback") String feedback,
                                         @RequestParam("recordDate")String recordDate) throws Exception {
        // 서비스에서 S3 업로드 실행
        log.info("파일 저장 컨트롤러 실행");
        try{
            Optional<users> user = userRepository.findById(uid);
            UserVideo userVideo = new UserVideo();
            userVideo.setUser(user.get());
            userVideo.setSportname(sportname);
            userVideo.setFeedback(feedback);
            userVideo.setRecordDate(LocalDate.parse(recordDate));

            String url = s3Service.uploadFile(file,"videos");
            return ResponseEntity.ok(url+" 파일을 업로드 했습니다.");
        } catch (Exception e) {
            log.error(" 파일 업로드 실패", e);
            return ResponseEntity.status(400).build();
        }
    }
}
