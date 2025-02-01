package com.example.homept

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.view.OpenGlView


class MainActivity : AppCompatActivity(), ConnectCheckerRtmp {

    private lateinit var openGlView: OpenGlView
    private lateinit var rtmpCamera2: RtmpCamera2
    private lateinit var btnStartStop: Button

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    private val PERMISSION_REQUEST_CODE = 123

    // 실제 테스트 시 서버 ip/도메인 + Stream Key 연동확인
    private val rtmpUrl = "rtmp://192.168.35.150/live/test"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openGlView = findViewById(R.id.openGlView)
        btnStartStop = findViewById(R.id.button)

        // RtmpCamera2(인자로 OpenGlView, ConnectCheckerRtmp(this))
        rtmpCamera2 = RtmpCamera2(openGlView, this)

        // 권한 체크
        checkPermissions()

        // 버튼으로 start/stop
        btnStartStop.setOnClickListener {
            if (!rtmpCamera2.isStreaming) {
                if (rtmpCamera2.isRecording || (rtmpCamera2.prepareVideo() && rtmpCamera2.prepareAudio())) {
                    rtmpCamera2.startStream(rtmpUrl)
                    btnStartStop.text = "Stop Streaming"
                } else {
                    Log.e("MainActivity", "Error preparing stream")
                }
            } else {
                rtmpCamera2.stopStream()
                btnStartStop.text = "Start Streaming"
            }
        }
    }

    private fun checkPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED

        if (!cameraGranted || !audioGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA_PERMISSION, AUDIO_PERMISSION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한 허용된 상태면 미리보기
            startPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startPreview()
            } else {
                // 권한 거부
                Log.e("MainActivity", "Camera/Audio permission denied")
            }
        }
    }

    private fun startPreview() {
        // 이미 프리뷰 중인지 확인
        if (!rtmpCamera2.isOnPreview) {
            // 기본 후면 카메라로 프리뷰 시작
            rtmpCamera2.startPreview()
        }
    }

    //-------------------------
    // ConnectCheckerRtmp 콜백 구현
    //-------------------------
    override fun onConnectionStartedRtmp(rtmpUrl: String) {
        Log.i("RTMP", "onConnectionStartedRtmp: $rtmpUrl")
    }

    override fun onConnectionSuccessRtmp() {
        Log.i("RTMP", "onConnectionSuccessRtmp")
    }

    override fun onConnectionFailedRtmp(reason: String) {
        Log.e("RTMP", "onConnectionFailedRtmp: $reason")
        runOnUiThread {
            if (rtmpCamera2.isStreaming) {
                rtmpCamera2.stopStream()
                btnStartStop.text = "Start Streaming"
            }
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        // 스트리밍 중 비트레이트 바뀌면 호출
    }

    override fun onDisconnectRtmp() {
        Log.w("RTMP", "onDisconnectRtmp")
    }

    override fun onAuthErrorRtmp() {
        Log.e("RTMP", "onAuthErrorRtmp")
    }

    override fun onAuthSuccessRtmp() {
        Log.i("RTMP", "onAuthSuccessRtmp")
    }
}
