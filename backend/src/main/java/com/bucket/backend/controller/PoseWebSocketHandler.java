package com.bucket.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

public class PoseWebSocketHandler extends TextWebSocketHandler {

    // 연결된 포즈 클라이언트 세션들을 저장
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("포즈 클라이언트 연결됨: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("포즈 메시지 수신: " + payload);
        JsonNode jsonNode = objectMapper.readTree(payload);
        String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

        // "pose" 타입이 아니면 무시
        if (!"pose".equals(type)) {
            System.out.println("유효하지 않은 포즈 메시지: " + payload);
            return;
        }

        // 포즈 데이터 처리 (필요에 따라 추가 로직 구현)
        System.out.println("MediaPipe 포즈 데이터 처리: " + jsonNode.toString());

        // 예: 연결된 다른 포즈 클라이언트에 브로드캐스트 (선택 사항)
        for (WebSocketSession s : sessions.values()) {
            if (!s.getId().equals(session.getId()) && s.isOpen()) {
                s.sendMessage(new TextMessage(payload));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("포즈 클라이언트 연결 종료: " + session.getId());
    }
}
