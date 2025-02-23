package com.bucket.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;

import java.io.IOException;
import java.util.Map;


//AI 모델과 WebSocket으로 실시간으로 통신하는 핸들러

@Service
@Slf4j
public class AIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Session aiSession;

    //사용할 관절 (필터링)
    private final List<String> Points = Arrays.asList(
            "Point_0", "Point_2", "Point_5", "Point_7", "Point_8",
            "Point_11", "Point_12", "Point_13", "Point_14", "Point_15",
            "Point_16", "Point_17", "Point_18", "Point_21", "Point_23",
            "Point_25", "Point_26", "Point_28", "Point_30", "Point_31", "Point_32"
    );

    public AIWebSocketHandler() {}

    //질문 답변받으면 추가할 예정
    // session: websocket 세션
    // message: json 메시지
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException{

        try{
            // Json 데이터를 Map 변환
            Map<String, Object> rawData = objectMapper.readValue(message.getPayload(), Map.class);
            log.info("Mediapipe 데이터 수신 : {}", rawData.toString());

            // AI Input형태로 Json 변환
            Map<String, Object> transformedData = convertJson(rawData);

            // 변환된 데이터 AI로 전송
            sendToAI(transformedData);
            
        } catch (IOException e) {
            log.error("WebSocket 메시지 처리 중 오류 발생", e);
        }
    }

    private Map<String,Object> convertJson(Map<String,Object> rawData){
        Map<String, Object> aiInput = new HashMap<>();

        return aiInput;
    }

    private void sendToAI(Map<String, Object> transformedData){
        try{
            String json = objectMapper.writeValueAsString(transformedData);

            log.info("AI 모델로 데이터 전송 완료: {}", json);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
