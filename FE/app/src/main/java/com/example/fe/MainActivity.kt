package com.example.fe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import org.json.JSONObject
import org.webrtc.*
import okhttp3.*
import okio.ByteString
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 1
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // WebRTC 관련
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var videoSource: VideoSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private var peerConnection: PeerConnection? = null
    private var signalingClient: SignalingClient? = null

    // 전면 or 후면 카메라 사용 여부 표시
    private var isFrontCameraUsed: Boolean = false

    // MediaPipe Pose
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var poseOverlayView: PoseOverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceViewRenderer = findViewById(R.id.surface_view)
        poseOverlayView = findViewById(R.id.poseOverlay)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE)
        } else {
            initAll()
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initAll()
        }
    }

    private fun initAll() {
        // 1) eglBase 먼저 초기화
        eglBase = EglBase.create()

        // 2) WebRTC 초기화
        initializeWebRTC()

        // 3) MediaPipe PoseLandmarker 초기화
        initializePoseLandmarker()
    }

    // =======================
    // WEBRTC 초기화
    // =======================
    private fun initializeWebRTC() {
        val initOptions = PeerConnectionFactory.InitializationOptions
            .builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        // 인코더/디코더 팩토리 (하드웨어 인코더)
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        // 전면 카메라 시, 미러 모드 켜고 싶다면 true
        // 후면이면 false
        // 여기서는, (isFrontCameraUsed==true)일 때 setMirror(true)로도 가능.
        surfaceViewRenderer.setMirror(true)

        startCameraCapture()
        createPeerConnection()

        signalingClient = SignalingClient { message -> onSignalingMessageReceived(message) }
        createOffer()
    }

    private fun startCameraCapture() {
        val enumerator = Camera2Enumerator(this)
        videoCapturer = createCameraCapturer(enumerator) ?: return

        videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        // 해상도 / FPS 상황에 맞게 조절 가능
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO_TRACK_ID", videoSource)
        localVideoTrack.addSink(surfaceViewRenderer)

        // Pose 분석 콜백
        localVideoTrack.addSink(object : VideoSink {
            override fun onFrame(frame: VideoFrame) {
                processWebRTCFrameWithPose(frame)
            }
        })
    }

    /**
     * 전면 카메라를 먼저 선택하는 createCameraCapturer()
     */
    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // 1) 전면 카메라 우선
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    isFrontCameraUsed = true
                    return capturer
                }
            }
        }

        // 2) 전면 카메라가 없거나 실패하면, 후면 카메라
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    isFrontCameraUsed = false
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

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
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
            })

        val streamId = "LOCAL_STREAM_ID"
        peerConnection?.addTrack(localVideoTrack, listOf(streamId))
    }

    private fun createOffer() {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
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
                val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                peerConnection?.setRemoteDescription(object : SdpObserver {
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
            "candidate" -> {
                val candidate = json.getString("candidate")
                val sdpMid = json.getString("sdpMid")
                val sdpMLineIndex = json.getInt("sdpMLineIndex")
                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                peerConnection?.addIceCandidate(iceCandidate)
            }
        }
    }

    // =======================
    // MediaPipe PoseLandmarker
    // =======================
    private fun initializePoseLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinPoseDetectionConfidence(0.8f)
            .setMinTrackingConfidence(0.8f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result: PoseLandmarkerResult, _: MPImage? ->
                handlePoseResult(result)
            }
            .setErrorListener { error ->
                Log.e("PoseLandmarker", "Error: $error")
            }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(this, options)
    }

    /**
     * WebRTC VideoFrame(I420)에 대해 회전 + (전면이면) 미러링 → ARGB → MediaPipe Pose
     */
    private fun processWebRTCFrameWithPose(frame: VideoFrame) {
        val i420Buffer = frame.buffer.toI420() ?: return
        val width = i420Buffer.width
        val height = i420Buffer.height
        val rotation = frame.rotation // 0,90,180,270

        if (width <= 0 || height <= 0) {
            i420Buffer.release()
            return
        }

        // 1) YUV→ARGB 변환
        val argbBytes = ByteArray(width * height * 4)
        convertI420ToARGB(
            i420Buffer.dataY, i420Buffer.dataU, i420Buffer.dataV,
            i420Buffer.strideY, i420Buffer.strideU, i420Buffer.strideV,
            argbBytes, width, height
        )
        i420Buffer.release()

        // 2) 회전 보정
        val rotated = rotateARGB(argbBytes, width, height, rotation)
        var finalArgb = rotated.data
        var finalW = rotated.width
        var finalH = rotated.height

        // 3) 전면 카메라인 경우, 추가로 미러링(좌우 뒤집기) 할 수도 있음
        if (isFrontCameraUsed) {
            val mirror = mirrorARGB(finalArgb, finalW, finalH)
            finalArgb = mirror
            // 크기는 동일
        }

        // 4) Bitmap 생성
        val bitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(finalArgb))

        // 5) MPImage 변환 & detectAsync
        val mpImage = BitmapImageBuilder(bitmap).build()
        poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())
    }

    /**
     * 포즈 결과 처리
     */
    private fun handlePoseResult(result: PoseLandmarkerResult) {
        val multiPoseLandmarks = result.landmarks()
        if (multiPoseLandmarks.isNotEmpty()) {
            val firstPose = multiPoseLandmarks[0]

            // 예: 평균 visibility 체크
            val avgVisibility = firstPose.map { it.visibility().orElse(0f) }.average()
            if (avgVisibility < 0.5) return

            runOnUiThread {
                poseOverlayView.updateLandmarks(firstPose)
            }
            val jsonData = convertLandmarksToJson(firstPose)
            signalingClient?.sendMessage(jsonData)
        }
    }

    private fun convertLandmarksToJson(landmarks: List<NormalizedLandmark>): String {
        val landmarksJson = landmarks.map { lm ->
            mapOf(
                "x" to lm.x(),
                "y" to lm.y(),
                "z" to lm.z(),
                "visibility" to (lm.visibility().orElse(0f))
            )
        }
        val jsonMap = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "landmarks" to landmarksJson
        )
        return JSONObject(jsonMap).toString()
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

        if (::poseLandmarker.isInitialized) {
            poseLandmarker.close()
        }
    }

    // ============================
    // 간단 I420 -> ARGB 변환
    // ============================
    private fun convertI420ToARGB(
        dataY: ByteBuffer,
        dataU: ByteBuffer,
        dataV: ByteBuffer,
        strideY: Int,
        strideU: Int,
        strideV: Int,
        outArgb: ByteArray,
        width: Int,
        height: Int
    ) {
        for (row in 0 until height) {
            val yOffset = row * strideY
            val uvOffset = (row / 2) * strideU
            for (col in 0 until width) {
                val y = (dataY.get(yOffset + col).toInt() and 0xFF)
                val u = (dataU.get(uvOffset + col / 2).toInt() and 0xFF) - 128
                val v = (dataV.get(uvOffset + col / 2).toInt() and 0xFF) - 128

                val r = (y + 1.370705f * v).roundToInt().coerceIn(0, 255)
                val g = (y - 0.698001f * v - 0.337633f * u).roundToInt().coerceIn(0, 255)
                val b = (y + 1.732446f * u).roundToInt().coerceIn(0, 255)

                val alpha = 255
                val pixelIndex = (row * width + col) * 4
                outArgb[pixelIndex + 0] = alpha.toByte()
                outArgb[pixelIndex + 1] = r.toByte()
                outArgb[pixelIndex + 2] = g.toByte()
                outArgb[pixelIndex + 3] = b.toByte()
            }
        }
    }

    // ======================================
    // ARGB 배열을 rotation각도에 따라 회전
    // ======================================
    data class RotatedData(val data: ByteArray, val width: Int, val height: Int)

    private fun rotateARGB(source: ByteArray, width: Int, height: Int, rotation: Int): RotatedData {
        return when (rotation) {
            0 -> RotatedData(source, width, height)
            90 -> {
                val rotated = rotateARGB90(source, width, height)
                RotatedData(rotated, height, width)
            }
            180 -> {
                val rotated = rotateARGB180(source, width, height)
                RotatedData(rotated, width, height)
            }
            270 -> {
                val rotated = rotateARGB270(source, width, height)
                RotatedData(rotated, height, width)
            }
            else -> RotatedData(source, width, height)
        }
    }

    private fun rotateARGB90(source: ByteArray, width: Int, height: Int): ByteArray {
        // 90도 회전: newWidth=height, newHeight=width
        val dest = ByteArray(width * height * 4)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcIndex = (row * width + col) * 4
                // 90도 -> (col, height-1-row)
                val dstCol = height - 1 - row
                val dstRow = col
                val dstIndex = (dstRow * height + dstCol) * 4
                // ARGB 4바이트 복사
                dest[dstIndex]     = source[srcIndex]
                dest[dstIndex + 1] = source[srcIndex + 1]
                dest[dstIndex + 2] = source[srcIndex + 2]
                dest[dstIndex + 3] = source[srcIndex + 3]
            }
        }
        return dest
    }

    private fun rotateARGB180(source: ByteArray, width: Int, height: Int): ByteArray {
        // 180도 회전
        val dest = ByteArray(width * height * 4)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcIndex = (row * width + col) * 4
                // 180도 -> (width-1-col, height-1-row)
                val dstRow = height - 1 - row
                val dstCol = width - 1 - col
                val dstIndex = (dstRow * width + dstCol) * 4
                dest[dstIndex]     = source[srcIndex]
                dest[dstIndex + 1] = source[srcIndex + 1]
                dest[dstIndex + 2] = source[srcIndex + 2]
                dest[dstIndex + 3] = source[srcIndex + 3]
            }
        }
        return dest
    }

    private fun rotateARGB270(source: ByteArray, width: Int, height: Int): ByteArray {
        // 270도 = 90도 3번
        val dest = ByteArray(width * height * 4)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcIndex = (row * width + col) * 4
                // 270도 -> (width-1-col, row)
                val dstCol = row
                val dstRow = width - 1 - col
                val dstIndex = (dstRow * height + dstCol) * 4
                dest[dstIndex]     = source[srcIndex]
                dest[dstIndex + 1] = source[srcIndex + 1]
                dest[dstIndex + 2] = source[srcIndex + 2]
                dest[dstIndex + 3] = source[srcIndex + 3]
            }
        }
        return dest
    }

    // ======================================
    // ARGB 배열을 좌우 미러링 (Front camera 용)
    // ======================================
    private fun mirrorARGB(source: ByteArray, width: Int, height: Int): ByteArray {
        // width, height 그대로
        // 좌우 반전: (x -> width-1-x)
        val dest = ByteArray(source.size)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcIndex = (row * width + col) * 4
                val dstCol = width - 1 - col
                val dstIndex = (row * width + dstCol) * 4
                dest[dstIndex]     = source[srcIndex]
                dest[dstIndex + 1] = source[srcIndex + 1]
                dest[dstIndex + 2] = source[srcIndex + 2]
                dest[dstIndex + 3] = source[srcIndex + 3]
            }
        }
        return dest
    }
}
