package com.bucket.backend.controller;

import com.bucket.backend.repository.UserVideoRepository;
import com.bucket.backend.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bucket.backend.model.UserVideo;
import com.bucket.backend.model.users;
import com.bucket.backend.repository.UserRepository;
import com.bucket.backend.service.S3Service;
import com.bucket.backend.service.UserVideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-videos")
public class UserVideoController {

    private static final Logger log = LoggerFactory.getLogger(UserVideoController.class);
    private final UserVideoService userVideoService;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final UserVideoRepository userVideoRepository;
    private final RedisService redisService;

    //@PostMapping
    //public ResponseEntity<UserVideo> createUserVideo(@RequestBody UserVideo userVideo) {}


    @PostMapping("/upload")
    public ResponseEntity<?> UploadVideo(@RequestParam MultipartFile file,
                                                     @RequestParam("uid") int uid) {
        log.info("녹화 영상 업로드 uid={}",uid);
        try{
            String url = s3Service.uploadFile(file,"videos");

            //redis에 저장하느 코드
            // Redis에 10분간 URL 저장 → 모바일이 GET 요청으로 가져가게
            //redisTemplate.opsForValue().set("temp:video:uid:" + uid, url, Duration.ofMinutes(10));
            redisService.saveUrl(uid, url);

            return ResponseEntity.ok("S3 url 저장 완료 : "+url);
        } catch (Exception e) {
            log.error("S3 업로드 실패",e);
            return ResponseEntity.status(500).body("업로드 실패 : "+ e.getMessage());
        }

    }

    // 운동 영상 업로드
    @PostMapping
    public ResponseEntity<?> saveRecord(@RequestParam("uid") int uid,
                                        @RequestParam("sportname") String sportname,
                                        @RequestParam("feedback") String feedback,
                                        @RequestParam("recordDate")String recordDate,
                                        @RequestParam("videoUrl") String videoUrl) {
        // 서비스에서 S3 업로드 실행
        log.info("운동 기록 저장 요청: uid={}, sport={}, date={}", uid, sportname, recordDate);
        try{
            Optional<users> userOpt = userRepository.findById(uid);
            //유저 존재 확인
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("유효하지 않은 사용자입니다.");
            }

            UserVideo video = new UserVideo();
            video.setUser(userOpt.get());
            video.setSportname(sportname);
            video.setFeedback(feedback);
            video.setRecordDate(LocalDate.parse(recordDate));
            video.setVideoUrl(videoUrl);

            userVideoRepository.save(video);

            //String url = s3Service.uploadFile(file,"videos");
            //UserVideo saveVideo = userVideoService.saveUserVideo(userVideo, url);

            return ResponseEntity.ok("운동 기록이 저장되었습니다.");
        } catch (Exception e) {
            log.error(" 파일 업로드 실패", e);
            return ResponseEntity.status(400).build();
        }
    }

    // 사용자별 운동 기록 조회 (관리자용 등)
    @GetMapping("/{uid}")
    public ResponseEntity<List<UserVideo>> getUserVideo(@PathVariable users uid) {
        List<UserVideo> videos = userVideoService.getVideoByUid(uid);
        return ResponseEntity.ok(videos);
    }

    // 운동 기록 상세 조회
    @GetMapping("/details/{vid}")
    public ResponseEntity<Optional<UserVideo>> getUserVideoDetail(@PathVariable int vid) {
        Optional<UserVideo> video = userVideoService.getUserVideoDetail(vid);
        return ResponseEntity.ok(video);
    }

    // 운동 기록 삭제
    @DeleteMapping("/{vid}")
    public ResponseEntity<?> deleteUserVideo(@PathVariable int vid) {
        userVideoService.deleteUserVideo(vid);
        return ResponseEntity.noContent().build();
    }

    // 운동 기록 수정
    @PutMapping("/{vid}")
    public ResponseEntity<?> updateUserVideo(@PathVariable int vid, @RequestBody UserVideo video) {
        UserVideo updateVideo = userVideoService.updateUserVideo(vid, video);
        return ResponseEntity.ok(updateVideo);
    }

    // ───────────────────────────────────────────────────────────
    // 로그인된 유저 자신의 운동 기록 전체 조회 기능 추가
    // GET /api/user-videos/me
    // ───────────────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyVideos(HttpSession session) {
        users user = (users) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("로그인 필요");
        }
        // 1) DB에서 해당 유저 기록 조회
        List<UserVideo> videos = userVideoService.getVideoByUid(user);

        // 2) 직접 HashMap으로 List<Map<String,Object>> 생성
        List<Map<String,Object>> result = new ArrayList<>();
        for (UserVideo v : videos) {
            Map<String,Object> row = new HashMap<>();
            row.put("vid",       v.getVid());
            row.put("date",      v.getRecordDate().toString());
            row.put("sportname", v.getSportname());
            row.put("videoUrl",  v.getVideoUrl());
            row.put("feedback",  v.getFeedback() != null ? v.getFeedback() : "");
            result.add(row);
        }

        // 3) 반환
        return ResponseEntity.ok(result);
    }


}
