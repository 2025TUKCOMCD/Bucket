package com.bucket.backend.controller;

import com.bucket.backend.websocket.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;

import java.io.IOException;
import java.util.Map;


//AI 모델과 WebSocket으로 실시간으로 통신하는 핸들러

public class AIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocket client = new WebSocket();


    //질문 답변받으면 추가할 예정
    // session: websocket 세션
    // message: json 메시지
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException{

        // Json 데이터를 Map 변환
        Map<String, Object> rawData = objectMapper.readValue(message.getPayload(), Map.class);

        // AI Input형태로 Json 변환
        Map<String, Object> transformedData = convertJson(rawData);

        // 변환된 데이터 AI로 전송
        sendToAI(transformedData);
    }

    private Map<String,Object> convertJson(Map<String,Object> rawData){
        Map<String, Object> aiInput = new HashMap<>();

        return aiInput;
    }

    private void sendToAI(Map<String, Object> transformedData){
        try{
            client.sendToAI(objectMapper.writeValueAsString(transformedData));
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
