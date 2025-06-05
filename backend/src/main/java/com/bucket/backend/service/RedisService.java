package com.bucket.backend.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    //TTL 10분
    public void saveUrl(int uid, String url) {
        String key = uid+"";
        redisTemplate.opsForValue().set(key, url, Duration.ofMinutes(10));
    }

    public String getUrl(int uid) {
        return redisTemplate.opsForValue().get(uid+"");
    }

    public void deleteUrl(int uid) {
        redisTemplate.delete(uid+"");
    }
}
