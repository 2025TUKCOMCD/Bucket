package com.example.fe

import okhttp3.*
import okio.ByteString

class SignalingClient(val listener: (String) -> Unit) {

    private val client = OkHttpClient()
    private val request = Request.Builder()
        // 에뮬레이터에서 테스트할 경우 호스트 PC의 localhost는 10.0.2.2 사용
        .url("ws://192.168.35.154:3000")
        .build()
    private val webSocket: WebSocket

    init {
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 연결 성공 시 필요한 처리를 추가할 수 있습니다.
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                listener(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener(bytes.utf8())
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket.send(message)
    }
}