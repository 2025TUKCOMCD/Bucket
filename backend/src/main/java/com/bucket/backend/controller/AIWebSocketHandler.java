package com.bucket.backend.controller;

import com.bucket.backend.websocket.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.handler.TextWebSocketHandler;


//AI 모델과 WebSocket으로 실시간으로 통신하는 핸들러
public class AIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocket client = new WebSocket();


    //
}
