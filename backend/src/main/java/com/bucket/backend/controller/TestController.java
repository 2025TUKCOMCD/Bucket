package com.bucket.backend.controller;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/test")
public class TestController {

    private final RedisTemplate<String, String> redisTemplate;

    public TestController(@Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/redis/set")
    public ResponseEntity<?> saveTest(@RequestParam String key,@RequestParam String value) {
        redisTemplate.opsForValue().set(key,value, Duration.ofMinutes(5));
        return ResponseEntity.ok("저장 완료");
    }

    @GetMapping("redis/get")
    public ResponseEntity<?> getTest(@RequestParam String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }
}
