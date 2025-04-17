package com.bucket.backend.controller;



import com.bucket.backend.model.UserVideo;
import com.bucket.backend.model.users;
import com.bucket.backend.service.UserVideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

//유저 비디오 관련 Controller
@RestController
@RequestMapping("/api/user-videos")
public class UserVideoController {

    private final UserVideoService userVideoService;

    public UserVideoController(UserVideoService userVideoService) {
        this.userVideoService = userVideoService;
    }

    //운동 영상 업로드
    @PostMapping
    public ResponseEntity<?> uploadVideo(@RequestBody UserVideo video){
        UserVideo saveVideo = userVideoService.saveUserVideo(video);
        return ResponseEntity.ok().body(
                new ApiResponse(saveVideo.getVid(), "운동 기록 데이터가 성공적으로 업로드 되었습니다.")
        );
    }

    // 사용자별 운동 기록 조회
    @GetMapping("/{uid}")
    public ResponseEntity<List<UserVideo>> getUserVideo(@PathVariable users uid){
        List<UserVideo> videos = userVideoService.getVideoByUid(uid);
        return ResponseEntity.ok(videos); //성공을 의미하는 OK(code 200)
        //ResponseEntity: HTTP 응답을 나타내는 spring framework의 클래스 -> 요청에 대한 응답의 상태 코드를 포함하여 클라이언트에게 전달한다.
    }

    // 운동 기록 상세 조회
    @GetMapping("/details/{vid}")
    public ResponseEntity<Optional<UserVideo>> getUserVideoDetail(@PathVariable int vid){
        Optional<UserVideo> video = userVideoService.getUserVideoDetail(vid);
        return ResponseEntity.ok().body(video);
    }


    @DeleteMapping("/{vid}")
    public ResponseEntity<?> deleteUserVideo(@PathVariable int vid, @RequestBody UserVideo video){
        userVideoService.deleteUserVideo(vid);
        return ResponseEntity.noContent().build(); //code 204 -> 클라이언트 요청은 정상적으로 처리되었지만 컨텐츠 제공은 X
    }

    @PutMapping("/{vid}")
    public ResponseEntity<?> updateUserVideo(@PathVariable int vid, @RequestBody UserVideo video){
        UserVideo updateVideo = userVideoService.updateUserVideo(vid, video);
        return ResponseEntity.ok().body(updateVideo);
    }


}
