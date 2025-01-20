package com.bucket.backend.repository;

import com.bucket.backend.model.UserVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserVideoRepository extends JpaRepository<UserVideo, Integer> {
    List<UserVideo> findByUserid(int userid);

}
