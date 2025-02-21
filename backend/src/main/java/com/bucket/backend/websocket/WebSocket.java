package com.bucket.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;


// AI모델과 Websocket 서버와 연결하여 데이터를 주고받는 Websocket 클라이언트
@ClientEndpoint
public class WebSocket {
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static Session aiSession;
    private final ObjectMapper objectMapper = new ObjectMapper();

    //AI모델과 Websocket의 연결 설정
    public WebSocket(){
        try {
            //WebSocketContainer: WebSocket 클라이언트의 서버연결, 세션관리
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
           // Websocket 연결 생성
            aiSession = container.connectToServer(this, new URI("ws://ai-server:5000/ws/analyze"));
        } catch(Exception e){
            log.error("error",e);
        }
    }

    //Ai모델에 데이터를 전송하는 메소드 (질문 답변 받으면 추가할 예쩡)
    public void sendToAI(String jsons) throws IOException{
        //메시지 전송
        aiSession.getBasicRemote().sendText(jsons);
    }
}
