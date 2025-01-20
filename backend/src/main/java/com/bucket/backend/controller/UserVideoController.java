package com.bucket.backend.controller;


//유저 비디오 관련 Controller

import com.bucket.backend.model.UserVideo;
import com.bucket.backend.service.UserVideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user-videos")
public class UserVideoController {

    private final UserVideoService userVideoService;

    public UserVideoController(UserVideoService userVideoService) {
        this.userVideoService = userVideoService;
    }

    @PostMapping
    public ResponseEntity<?> uploadVideo(@RequestBody UserVideo video){
        UserVideo saveVideo = userVideoService.saveUserVideo(video);
        return ResponseEntity.ok().body(saveVideo);

    }
}
