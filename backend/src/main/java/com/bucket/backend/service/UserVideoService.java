package com.bucket.backend.service;

import com.bucket.backend.model.UserVideo;
import com.bucket.backend.model.users;
import com.bucket.backend.repository.UserRepository;
import com.bucket.backend.repository.UserVideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserVideoService {

    private final UserVideoRepository userVideoRepository;
    private final UserRepository userRepository;

    public UserVideoService(UserVideoRepository userVideoRepository, UserRepository userRepository) {
        this.userVideoRepository = userVideoRepository;
        this.userRepository = userRepository;
    }

    //운동 영상 저장
    public UserVideo saveUserVideo(UserVideo userVideo) {
        // 유저 확인 예외 처리
        if(!userRepository.existsById(userVideo.getUser().getUid())){
            throw new IllegalArgumentException("유저를 찾지 못했습니다.");
        }
        return userVideoRepository.save(userVideo);
    }

    // 사용자별 운동 기록 조회
    public List<UserVideo> getVideoByUid(users uid) {
        return userVideoRepository.findByUser(uid);
    }

    //운동 기록 상세 조회
    public Optional<UserVideo> getUserVideoDetail(int vid) {
        return userVideoRepository.findById(vid);
    }
}
