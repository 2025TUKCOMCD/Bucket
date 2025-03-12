package com.example.fe

import okhttp3.*
import okio.ByteString

class SignalingClient(
    // 시그널링 서버에서 오는 메시지를 받을 콜백
    private val signalingListener: (String) -> Unit,
    // 포즈 서버에서 오는 메시지를 받을 콜백
    private val poseListener: (String) -> Unit
) {

    private val client = OkHttpClient()

    // 시그널링 서버용 WebSocket
    private val requestSignaling = Request.Builder()
        .url("ws://15.165.138.10:8080/signaling")
        .build()

    // 포즈 서버용 WebSocket
    private val requestPose = Request.Builder()
        .url("ws://15.165.138.10:8080/pose")
        .build()

    // 실제 WebSocket 객체(시그널링용, 포즈용) 2개
    private val signalingWebSocket: WebSocket
    private val poseWebSocket: WebSocket

    init {
        // 시그널링용
        signalingWebSocket = client.newWebSocket(requestSignaling, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 연결 성공 시
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 시그널링 메시지를 전달
                signalingListener(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                signalingListener(bytes.utf8())
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })

        // 포즈용
        poseWebSocket = client.newWebSocket(requestPose, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 연결 성공 시
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 포즈 데이터 메시지를 전달
                poseListener(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                poseListener(bytes.utf8())
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })
    }

    // 시그널링 서버로 메시지 전송 (offer, answer, candidate 등)
    fun sendSignalingMessage(message: String) {
        signalingWebSocket.send(message)
    }

    // 포즈 서버로 메시지 전송 (MediaPipe pose 데이터 등)
    fun sendPoseMessage(message: String) {
        poseWebSocket.send(message)
    }
}
