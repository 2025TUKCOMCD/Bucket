package com.example.fe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.webrtc.*
import okhttp3.*
import okio.ByteString

class MainActivity : AppCompatActivity() {

    // 권한 관련
    private val PERMISSIONS_REQUEST_CODE = 1
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // WebRTC 관련 변수들
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var videoSource: VideoSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private lateinit var peerConnection: PeerConnection

    // 시그널링 클라이언트 (WebSocket 기반)
    private var signalingClient: SignalingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
        } else {
            initializeWebRTC()
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeWebRTC()
            } else {
                // 권한 거부 시 처리 (예: 토스트 표시)
            }
        }
    }

    private fun initializeWebRTC() {
        // PeerConnectionFactory 초기화
        val initializationOptions = PeerConnectionFactory.InitializationOptions
            .builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        eglBase = EglBase.create()

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        // SurfaceViewRenderer 초기화 (미리보기)
        surfaceViewRenderer = findViewById(R.id.surface_view)
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setMirror(true)

        // 카메라 캡처 시작
        startCameraCapture()

        // PeerConnection 생성
        createPeerConnection()

        // 시그널링 서버 연결 (WebSocket)
        signalingClient = SignalingClient { message ->
            onSignalingMessageReceived(message)
        }

        // SDP Offer 생성 및 전송
        createOffer()
    }

    private fun startCameraCapture() {
        videoCapturer = createCameraCapturer() ?: return

        videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO_TRACK_ID", videoSource)
        localVideoTrack.addSink(surfaceViewRenderer)
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames

        // 후면 카메라 우선
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }
        // 후면 카메라가 없으면 전면 카메라
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }
        return null
    }


    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                // ICE 후보를 시그널링 서버로 전송
                sendIceCandidate(candidate)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            // ★ remove onCreateDataChannel(...) if "overrides nothing" error occurs
        }) ?: return


        // Unified Plan에서는 addStream() 대신 addTrack()을 사용
        val streamId = "LOCAL_STREAM_ID"
        peerConnection.addTrack(localVideoTrack, listOf(streamId))
    }

    private fun createOffer() {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        sendSdpOfferToServer(sdp)
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Set local description failed: $error")
                    }
                    override fun onCreateSuccess(sdp: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "Create offer failed: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sdpConstraints)
    }

    private fun onAnswerReceived(sdpAnswer: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d("WebRTC", "Remote description set successfully")
            }
            override fun onSetFailure(error: String?) {
                Log.e("WebRTC", "Set remote description failed: $error")
            }
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sessionDescription)
    }

    private fun sendSdpOfferToServer(sdp: SessionDescription) {
        val message = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        signalingClient?.sendMessage(message.toString())
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val message = JSONObject().apply {
            put("type", "candidate")
            put("candidate", candidate.sdp)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("sdpMid", candidate.sdpMid)
        }
        signalingClient?.sendMessage(message.toString())
    }


    private fun onSignalingMessageReceived(message: String) {
        val json = JSONObject(message)
        when (json.getString("type")) {
            "answer" -> {
                val sdp = json.getString("sdp")
                onAnswerReceived(sdp)
            }
            "candidate" -> {
                val candidate = json.getString("candidate")
                val sdpMid = json.getString("sdpMid")
                val sdpMLineIndex = json.getInt("sdpMLineIndex")
                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                peerConnection.addIceCandidate(iceCandidate)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            videoCapturer.stopCapture()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        surfaceViewRenderer.release()
        peerConnectionFactory.dispose()
        eglBase.release()
    }
}
