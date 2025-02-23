package com.bucket.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;


// AI모델과 Websocket 서버와 연결하여 데이터를 주고받는 Websocket 클라이언트
@ClientEndpoint
public class WebSocket {
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static Session aiSession;
    private final ObjectMapper objectMapper = new ObjectMapper();


    //사용할 관절 리스트 (필터링 기능)
    private static final List<String> selectedPoints = Arrays.asList(
            "Point_0", "Point_2", "Point_5", "Point_7", "Point_8",
            "Point_11", "Point_12", "Point_13", "Point_14", "Point_15",
            "Point_16", "Point_17", "Point_18", "Point_21", "Point_23",
            "Point_25", "Point_26", "Point_28", "Point_30", "Point_31", "Point_32"
    );


    //AI모델과 Websocket의 연결 설정
    public WebSocket(){
        try {
            //WebSocketContainer: WebSocket 클라이언트의 서버연결, 세션관리
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
           // Websocket 연결 생성
            aiSession = container.connectToServer(this, new URI("ws://localhost:5000/ws/connect"));
            log.info("연결 성공: ws://localhost:5000/ws/connect");
        } catch(Exception e){
            log.error("error",e);
        }
    }

    //Ai모델에 데이터를 전송하는 메소드 (질문 답변 받으면 추가할 예쩡)
    public void sendToAI(String jsons) throws IOException{
        //메시지 전송
        // 테스트 JSON 데이터
        String testJson = "{"
                + "\"user_id\": 1,"
                + "\"exercise\": \"push-up\","
                + "\"keypoints\": {"
                + "    \"Point_0\": {\"x\": 0.1, \"y\": 0.2},"
                + "    \"Point_1\": {\"x\": 0.3, \"y\": 0.4}"
                + "}"
                + "}";

        aiSession.getBasicRemote().sendText(testJson);
    }

    //public void convertJson

    @OnMessage
    public void onMessage(String message){
        System.out.println("응답 수신: "+ message);
    }
}
