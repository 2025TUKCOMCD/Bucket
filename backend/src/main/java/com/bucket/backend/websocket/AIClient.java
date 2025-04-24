package com.bucket.backend.websocket;


import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bucket.backend.controller.AIWebSocketHandler.session;

@Slf4j
@ClientEndpoint
@Component
public class AIClient{
    private static Session aiSession;
    //ai서버 URL
    private final String AI_URL = "ws://localhost:5000/ws/connect";

    // 연결된 클라이언트(스마트폰)의 WebSocket 세션 저장
    private static final ConcurrentHashMap<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();

    public AIClient(){
        //AI서버와 연결 설정
        try{
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            aiSession = container.connectToServer(this, new URI(AI_URL));
            log.info("AI 서버 연결 성공!");
        }catch (Exception e){
            log.error("AI websocket 연결 실패!",e);
        }
    }

    @OnOpen
    public void onOpen(Session session){
        log.info("AI와 연결됨");
    }

    @OnMessage
    public void onMessage(String message){
        log.info("AI 응답 수신: {}",message);
        try{
            for(WebSocketSession session : clientSessions.values()){
                if(session!= null && session.isOpen()){
                    session.sendMessage(new TextMessage(message));
                    log.info("클라이언트로 전달 완료");
                }
            }
        } catch (IOException e) {
            log.error("전달 중 오류 발생",e);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable){
        log.error("AI Websocket 오류 발생",throwable.getMessage());
    }
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        log.warn("AI WebSocket 연결 종료됨: {}", reason);
    }

    public void sendToAI(String json)throws IOException{
        try{
            if(aiSession != null && aiSession.isOpen()){
                aiSession.getBasicRemote().sendText(json);
                log.info("AI 모델로 데이터 전송: {}",json);
            }else{
                log.error("AI Socket 세션 닫혀 있음.");
            }
        } catch (IOException e){
            log.error("메시지 전송 중 오류 발생", e);
        }

    }
    // 클라이언트 세션을 등록하는 메서드
    public static void registerClientSession(String clientId, WebSocketSession session) {
        clientSessions.put(clientId, session);
    }

    // 클라이언트 세션을 제거하는 메서드
    public static void removeClientSession(String clientId) {
        clientSessions.remove(clientId);
    }
}
