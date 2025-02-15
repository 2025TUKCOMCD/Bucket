package com.bucket.backend.controller;

import com.bucket.backend.websocket.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;


//AI 모델과 WebSocket으로 실시간으로 통신하는 핸들러

public class AIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocket client = new WebSocket();


    //질문 답변받으면 추가할 예정

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException{

        //AI모델로 데이터 전송
        client.sendToAI();
    }
}
