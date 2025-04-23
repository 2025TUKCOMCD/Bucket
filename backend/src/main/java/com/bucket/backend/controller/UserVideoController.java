package com.bucket.backend.controller;

import com.bucket.backend.controller.ApiResponse;
import com.bucket.backend.model.UserVideo;
import com.bucket.backend.model.users;
import com.bucket.backend.service.UserVideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-videos")
public class UserVideoController {

    private final UserVideoService userVideoService;

    public UserVideoController(UserVideoService userVideoService) {
        this.userVideoService = userVideoService;
    }

    // 운동 영상 업로드
    @PostMapping
    public ResponseEntity<?> uploadVideo(@RequestBody UserVideo video) {
        UserVideo saveVideo = userVideoService.saveUserVideo(video);
        return ResponseEntity.ok().body(
                new ApiResponse(saveVideo.getVid(), "운동 기록 데이터가 성공적으로 업로드 되었습니다.")
        );
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
            row.put("sportname", v.getSportname());
            row.put("date",      v.getRecordDate().toString());
            row.put("feedback",  v.getFeedback() != null ? v.getFeedback() : "");
            result.add(row);
        }

        // 3) 반환
        return ResponseEntity.ok(result);
    }
}
