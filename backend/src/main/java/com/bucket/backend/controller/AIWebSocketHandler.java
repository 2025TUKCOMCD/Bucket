package com.bucket.backend.controller;

import com.bucket.backend.websocket.AIClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;

import java.io.IOException;
import java.util.Map;

// AI 모델과 WebSocket으로 실시간으로 통신하는 핸들러
@Service
@Slf4j
public class AIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static WebSocketSession session; // Spring WebSocket 세션 관리
    private final AIClient aiClient;  //AI서버와 연결된 session
    @Getter
    @Setter
    private static String selectedExercise = "pushup"; //기본값

    // 사용할 관절 (푸쉬업)
    private final List<String> Points_1 = Arrays.asList(
            "Point_0", "Point_7", "Point_8", "Point_11", "Point_12",
            "Point_13", "Point_14", "Point_15", "Point_16", "Point_17",
            "Point_18", "Point_21", "Point_22", "Point_23", "Point_24",
            "Point_25", "Point_26", "Point_27", "Point_28", "Point_29", "Point_30"
    );

    // 사용할 관절 (런지)
    private final List<String> Points_2 = Arrays.asList(
            "Point_0", "Point_7", "Point_11", "Point_12", "Point_23", "Point_24",
            "Point_25", "Point_26", "Point_27", "Point_28", "Point_29", "Point_30",
            "Point_31", "Point_32"
    );


    @Autowired
    public AIWebSocketHandler(AIClient aiClient) {
        this.aiClient = aiClient;
        log.info("핸들러 인스턴스 생성");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("클라이언트 연결됨: {}", session.getId());
        //클라이언트 세션 등록
        AIClient.registerClientSession(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("클라이언트 연결 종료: {}", session.getId());
        // 클라이언트 세션 제거
        AIClient.removeClientSession(session.getId());
    }

    // WebSocket으로 받은 메시지 처리
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            // JSON 데이터를 Map으로 변환
            Map<String, Object> rawData = objectMapper.readValue(message.getPayload(), Map.class);
            log.info("Mediapipe 데이터 수신: {}", rawData);

            // AI Input형태로 변환
            Map<String, Object> transformedData = convertJson(rawData);

            log.info("변환된 JSON: {}", transformedData);
            // 변환된 데이터 AI로 전송
            sendToAI(transformedData);

        } catch (IOException e) {
            log.error("WebSocket 메시지 처리 중 오류 발생", e);
        }
    }

    // JSON 변환 로직
    private Map<String, Object> convertJson(Map<String, Object> rawData) {
        Map<String, Object> aiInput = new LinkedHashMap<>();
        List<Map<String, Object>> filteredFrames = new ArrayList<>();

        List<Map<String, Object>> frames = (List<Map<String, Object>>) rawData.get("frames");


        String viewKey = getSelectedExercise().equals("pushup") ? "view3" : "view4";
        List<String> selectedPoints = getSelectedExercise().equals("pushup") ? Points_1 : Points_2;

        log.info("현재 운동 : {}",viewKey);

        for (Map<String, Object> frame : frames) {
            Map<String, Object> newFrame = new LinkedHashMap<>();
            Map<String, Object> viewData = (Map<String, Object>) frame.get("view");

            if (viewData != null) {
                Map<String, Object> pts = (Map<String, Object>) viewData.get("pts");

                if (pts != null) {
                    Map<String, Object> filteredPts = new LinkedHashMap<>();

                    // 필터링할 포인트만 선택
                    for (String key : selectedPoints) {
                        if (pts.containsKey(key)) {
                            Map<String, Object> point = (Map<String, Object>) pts.get(key);
                            Map<String, Object> filteredPoint = new LinkedHashMap<>();

                            filteredPoint.put("x", point.get("x"));
                            filteredPoint.put("y", point.get("y"));

                            if("lunge".equals(getSelectedExercise()) && point.containsKey("z")) {
                                filteredPoint.put("z", point.get("z"));
                            }

                            filteredPts.put(key, filteredPoint);
                        }
                    }

                    Map<String, Object> newViewData = new LinkedHashMap<>();
                    newViewData.put("pts", filteredPts);

                    newFrame.put(viewKey, newViewData);
                    filteredFrames.add(newFrame);
                }
            }
        }

        aiInput.put("type","pose");
        aiInput.put("frames", filteredFrames);

        return aiInput;
    }

    private void sendToAI(Map<String, Object> transformedData) {
        try {
            String json = objectMapper.writeValueAsString(transformedData);
            aiClient.sendToAI(json);
            //log.info("AI 모델로 데이터 전송 완료: {}", json);
            }
        catch (IOException e) {
            log.error("AI로 데이터 전송 중 오류 발생", e);
        }
    }

}


