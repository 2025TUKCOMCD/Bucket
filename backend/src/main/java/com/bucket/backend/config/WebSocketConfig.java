package com.bucket.backend.config;

import com.bucket.backend.controller.PoseWebSocketHandler;
import com.bucket.backend.controller.SignalingWebSocketHandler;
import com.bucket.backend.controller.AIWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

//Spring Boot에서 WebSocket을 활성화하는 설정 클래스
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AIWebSocketHandler aiWebSocketHandler;

    @Autowired
    public WebSocketConfig(AIWebSocketHandler aiWebSocketHandler) {
        this.aiWebSocketHandler = aiWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){



        registry.addHandler(aiWebSocketHandler, "/ws/connect").setAllowedOrigins("*");
        // 영상 데이터용 엔드포인트
        registry.addHandler(new SignalingWebSocketHandler(), "/signaling").setAllowedOrigins("*");
        // 포즈 데이터 전용 엔드포인트
        registry.addHandler(new PoseWebSocketHandler(), "/pose").setAllowedOrigins("*");

    }


}