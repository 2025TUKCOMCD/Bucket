// MainActivity.kt
package com.example.fe

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
    }

    // ─── 권한 ─────────────────────────────────────────────────
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    // ─── WebRTC ───────────────────────────────────────────────
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var videoSource: VideoSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var surfaceViewRenderer: SurfaceViewRenderer
    private lateinit var eglBase: EglBase
    private var peerConnection: PeerConnection? = null
    private var isFrontCameraUsed: Boolean = false

    // ─── 시그널링 & 포즈 WebSocket ────────────────────────────
    private var signalingClient: SignalingClient? = null

    // ─── MediaPipe Pose ───────────────────────────────────────
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var poseOverlayView: PoseOverlayView

    // Pose 전송 간격 제어
    private val POSE_SEND_INTERVAL_MS = 333L
    private var lastPoseSendTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceViewRenderer = findViewById(R.id.surface_view)
        poseOverlayView      = findViewById(R.id.poseOverlay)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this, requiredPermissions, PERMISSIONS_REQUEST_CODE
            )
        } else {
            initAll()
        }

        // 촬영 종료 버튼: 운동 선택 → 업로드로 이동
        findViewById<Button>(R.id.btnFinish).setOnClickListener {
            showExerciseSelectionDialog()
        }
    }

    private fun hasPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE &&
            grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        ) {
            initAll()
        }
    }

    private fun initAll() {
        eglBase = EglBase.create()
        initializeWebRTC()
        initializePoseLandmarker()
    }

    // ─── WebRTC 초기화 ─────────────────────────────────────────
    private fun initializeWebRTC() {
        val initOptions =
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setMirror(true)

        startCameraCapture()
        createPeerConnection()

        signalingClient = SignalingClient(
            signalingListener = { msg -> onSignalingMessageReceived(msg) },
            poseListener      = { msg -> onPoseMessageReceived(msg) }
        )
        createOffer()
    }

    private fun startCameraCapture() {
        val enumerator = Camera2Enumerator(this)
        val frontName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        videoCapturer = if (frontName != null) {
            isFrontCameraUsed = true
            enumerator.createCapturer(frontName, null)
        } else {
            val backName = enumerator.deviceNames.first { !enumerator.isFrontFacing(it) }
            isFrontCameraUsed = false
            enumerator.createCapturer(backName, null)
        }

        videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO_TRACK_ID", videoSource)
        localVideoTrack.addSink(surfaceViewRenderer)
        localVideoTrack.addSink { frame -> processWebRTCFrameWithPose(frame) }
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        peerConnection = peerConnectionFactory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServers),
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    sendIceCandidate(candidate)
                }
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
                override fun onAddStream(stream: MediaStream) {}
                override fun onRemoveStream(stream: MediaStream) {}
                override fun onDataChannel(dc: DataChannel) {}
                override fun onRenegotiationNeeded() {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            }
        )?.apply {
            addTrack(localVideoTrack, listOf("LOCAL_STREAM_ID"))
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
            )
            mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
            )
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        sendSdpOfferToServer(sdp)
                    }
                    override fun onSetFailure(error: String?) = Unit
                    override fun onCreateSuccess(sdp: SessionDescription?) = Unit
                    override fun onCreateFailure(error: String?) = Unit
                }, sdp)
            }
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
    }

    private fun sendSdpOfferToServer(sdp: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        signalingClient?.sendSignalingMessage(msg.toString())
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val msg = JSONObject().apply {
            put("type", "candidate")
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        signalingClient?.sendSignalingMessage(msg.toString())
    }

    private fun onSignalingMessageReceived(message: String) {
        val json = JSONObject(message)
        when (json.getString("type")) {
            "answer" -> {
                val sdp  = json.getString("sdp")
                val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                    override fun onCreateSuccess(sdp: SessionDescription?) = Unit
                    override fun onCreateFailure(error: String?) = Unit
                }, desc)
            }
            "candidate" -> {
                val candidate = IceCandidate(
                    json.getString("sdpMid"),
                    json.getInt("sdpMLineIndex"),
                    json.getString("candidate")
                )
                peerConnection?.addIceCandidate(candidate)
            }
        }
    }

    // ─── PoseLandmarker 초기화 ───────────────────────────────────
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

    private fun processWebRTCFrameWithPose(frame: VideoFrame) {
        val i420   = frame.buffer.toI420() ?: return
        val width  = i420.width
        val height = i420.height
        val yuv    = ByteArray(width * height * 4)
        convertI420ToARGB(
            i420.dataY, i420.dataU, i420.dataV,
            i420.strideY, i420.strideU, i420.strideV,
            yuv, width, height
        )
        i420.release()
        val rotated = rotateARGB(yuv, width, height, frame.rotation)
        var data = rotated.data
        var w    = rotated.width
        var h    = rotated.height
        if (isFrontCameraUsed) data = mirrorARGB(data, w, h)
        val bmp   = android.graphics.Bitmap.createBitmap(
            w, h, android.graphics.Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        val mpImage = BitmapImageBuilder(bmp).build()
        poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())
    }

    private fun handlePoseResult(result: PoseLandmarkerResult) {
        val landmarks = result.landmarks().firstOrNull() ?: return
        val avgVis    = landmarks.map { it.visibility().orElse(0f) }.average()
        if (avgVis < 0.3) return
        runOnUiThread { poseOverlayView.updateLandmarks(landmarks) }
        val now = System.currentTimeMillis()
        if (now - lastPoseSendTime >= POSE_SEND_INTERVAL_MS) {
            lastPoseSendTime = now
            val json = convertPoseToJson(landmarks)
            signalingClient?.sendPoseMessage(json)
        }
    }

    private fun convertPoseToJson(
        landmarks: List<NormalizedLandmark>
    ): String {
        val pts = landmarks.mapIndexed { i, lm ->
            "Point_$i" to mapOf("x" to lm.x(), "y" to lm.y())
        }.toMap()
        val view   = mapOf("pts" to pts)
        val frames = listOf(mapOf("view" to view))
        return JSONObject(mapOf("type" to "pose", "frames" to frames)).toString()
    }

    // ─── I420 → ARGB 변환 ───────────────────────────────────────
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
            val yOffset  = row * strideY
            val uvOffset = (row / 2) * strideU
            for (col in 0 until width) {
                val y = dataY.get(yOffset + col).toInt() and 0xFF
                val u = (dataU.get(uvOffset + col / 2).toInt() and 0xFF) - 128
                val v = (dataV.get(uvOffset + col / 2).toInt() and 0xFF) - 128
                val r = (y + 1.370705f * v).roundToInt().coerceIn(0, 255)
                val g = (y - 0.698001f * v - 0.337633f * u).roundToInt().coerceIn(0, 255)
                val b = (y + 1.732446f * u).roundToInt().coerceIn(0, 255)
                val idx = (row * width + col) * 4
                outArgb[idx + 0] = 255.toByte()
                outArgb[idx + 1] = r.toByte()
                outArgb[idx + 2] = g.toByte()
                outArgb[idx + 3] = b.toByte()
            }
        }
    }

    private data class RotatedData(val data: ByteArray, val width: Int, val height: Int)

    private fun rotateARGB(
        source: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ): RotatedData {
        return when (rotation) {
            0 -> RotatedData(source, width, height)
            90 -> {
                val dst = rotateARGB90(source, width, height)
                RotatedData(dst, height, width)
            }
            180 -> RotatedData(rotateARGB180(source, width, height), width, height)
            270 -> {
                val dst = rotateARGB270(source, width, height)
                RotatedData(dst, height, width)
            }
            else -> RotatedData(source, width, height)
        }
    }

    private fun rotateARGB90(source: ByteArray, width: Int, height: Int): ByteArray {
        val dest = ByteArray(source.size)
        for (r in 0 until height) {
            for (c in 0 until width) {
                val si = (r * width + c) * 4
                val dr = c
                val dc = height - 1 - r
                val di = (dr * height + dc) * 4
                for (i in 0..3) dest[di + i] = source[si + i]
            }
        }
        return dest
    }

    private fun rotateARGB180(source: ByteArray, width: Int, height: Int): ByteArray {
        val dest = ByteArray(source.size)
        for (r in 0 until height) {
            for (c in 0 until width) {
                val si = (r * width + c) * 4
                val dr = height - 1 - r
                val dc = width - 1 - c
                val di = (dr * width + dc) * 4
                for (i in 0..3) dest[di + i] = source[si + i]
            }
        }
        return dest
    }

    private fun rotateARGB270(source: ByteArray, width: Int, height: Int): ByteArray {
        val dest = ByteArray(source.size)
        for (r in 0 until height) {
            for (c in 0 until width) {
                val si = (r * width + c) * 4
                val dr = width - 1 - c
                val dc = r
                val di = (dr * height + dc) * 4
                for (i in 0..3) dest[di + i] = source[si + i]
            }
        }
        return dest
    }

    private fun mirrorARGB(source: ByteArray, width: Int, height: Int): ByteArray {
        val dest = ByteArray(source.size)
        for (r in 0 until height) {
            for (c in 0 until width) {
                val si = (r * width + c) * 4
                val dc = width - 1 - c
                val di = (r * width + dc) * 4
                for (i in 0..3) dest[di + i] = source[si + i]
            }
        }
        return dest
    }

    // ─── 운동 선택 다이얼로그 & 업로드 이동 ───────────────────
    private fun showExerciseSelectionDialog() {
        val options = arrayOf("푸쉬업", "런지")
        AlertDialog.Builder(this)
            .setTitle("운동 종류 선택")
            .setItems(options) { _, which ->
                val code = if (which == 0) "pushup" else "lunge"
                navigateToUpload(code)
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToUpload(selectedSport: String) {
        Intent(this, UploadActivity::class.java).also {
            it.putExtra("sportname", selectedSport)
            startActivity(it)
        }
        finish()
    }

    // ─── 백엔드 AI 응답 처리 ───────────────────────────────────
    private fun onPoseMessageReceived(message: String) {
        Log.d("MainActivity", "AI 응답: $message")
        runOnUiThread {
            Toast.makeText(this, "AI: $message", Toast.LENGTH_SHORT).show()
            // 또는, 레이아웃에 TextView(R.id.resultTextView)가 있으면:
            // findViewById<TextView>(R.id.resultTextView).text =
            //     JSONObject(message).optString("prediction_result", "")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { videoCapturer.stopCapture() } catch (_: Exception) {}
        surfaceViewRenderer.release()
        peerConnectionFactory.dispose()
        eglBase.release()
        if (::poseLandmarker.isInitialized) poseLandmarker.close()
        signalingClient?.disconnect()
    }
}
