package com.bucket.backend.repository;

import com.bucket.backend.model.user_videos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVideoRepository extends JpaRepository<user_videos, Integer> {
}
