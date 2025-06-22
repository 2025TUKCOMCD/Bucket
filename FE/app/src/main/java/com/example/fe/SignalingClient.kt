package com.example.fe

import okhttp3.*
import okio.ByteString

class SignalingClient(
    private val signalingListener: (String) -> Unit,
    private val poseListener: (String) -> Unit
) {
    private val client = OkHttpClient()

    private val requestSignaling = Request.Builder()
        .url("wss:/homept.online/signaling")
        .build()
    private val requestPose = Request.Builder()
        .url("wss://homept.online/pose")
        .build()

    private val signalingWebSocket: WebSocket
    private val poseWebSocket: WebSocket

    init {
        signalingWebSocket = client.newWebSocket(requestSignaling, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, resp: Response) {}
            override fun onMessage(ws: WebSocket, text: String) {
                signalingListener(text)
            }
            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                signalingListener(bytes.utf8())
            }
            override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                t.printStackTrace()
            }
        })
        poseWebSocket = client.newWebSocket(requestPose, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, resp: Response) {}
            override fun onMessage(ws: WebSocket, text: String) {
                poseListener(text)
            }
            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                poseListener(bytes.utf8())
            }
            override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                t.printStackTrace()
            }
        })
    }

    fun sendSignalingMessage(message: String) {
        signalingWebSocket.send(message)
    }
    fun sendPoseMessage(message: String) {
        poseWebSocket.send(message)
    }

    /** MainActivity.onDestroy() 에서 호출 **/
    fun disconnect() {
        signalingWebSocket.close(1000, "Activity destroyed")
        poseWebSocket.close(1000, "Activity destroyed")
        client.dispatcher.executorService.shutdown()
    }
}
