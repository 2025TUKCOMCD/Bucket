package com.bucket.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

public class SignalingWebSocketHandler extends TextWebSocketHandler {

    // 연결된 세션들을 저장
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("클라이언트 연결됨: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 수신된 전체 원본 메시지 출력
        String payload = message.getPayload();
        System.out.println("수신한 메시지: " + payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

            if ("pose".equals(type)) {
                System.out.println("MediaPipe 포즈 데이터 수신: " + jsonNode.toString());
            } else {
                switch (type) {
                    case "offer":
                        System.out.println("offer 수신");
                        break;
                    case "answer":
                        System.out.println("answer 수신");
                        break;
                    case "candidate":
                        System.out.println("ICE candidate 수신");
                        break;
                    default:
                        System.out.println("기타 메시지 수신: " + jsonNode.toString());
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("JSON 파싱 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        // 브로드캐스트: 같은 세션이 아니고, 세션이 열려 있다면 전송
        for (WebSocketSession s : sessions.values()) {
            if (!s.getId().equals(session.getId()) && s.isOpen()) {
                s.sendMessage(new TextMessage(payload));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("클라이언트 연결 종료됨: " + session.getId());
    }
}
