package com.example.cameravideoapp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recordButton: Button
    private lateinit var previewView: androidx.camera.view.PreviewView
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        recordButton = findViewById(R.id.recordButton)

        // 🔹 권한 확인 후 카메라 시작
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        recordButton.setOnClickListener {
            if (allPermissionsGranted()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }
    }
    // 연동확인
    // 🔹 필수 권한 목록 
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    // 🔹 모든 권한이 허용되었는지 확인
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 🔹 권한 요청 함수
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    // 🔹 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d("Permission", "모든 권한이 허용됨")
                startCamera()
            } else {
                Log.e("Permission", "권한 거부됨")
                Toast.makeText(this, "앱 실행을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        try {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                return
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                } catch (exc: Exception) {
                    Log.e("CameraX", "카메라 실행 실패", exc)
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: SecurityException) {
            Log.e("Permission", "권한 없음: ${e.message}")
            requestPermissions()
        }
    }

    private fun startRecording() {
        try {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "녹화하려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                return
            }

            val videoCapture = this.videoCapture ?: return

            if (recording != null) {
                recording?.stop()
                recording = null
                recordButton.text = "녹화 시작"
                return
            }

            val videoFile = File(
                externalCacheDir,
                "video_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
            )

            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            recordButton.text = "녹화 중지"
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                Log.e("MainActivity", "녹화 실패: ${recordEvent.cause?.message}")
                            } else {
                                Log.d("MainActivity", "녹화 완료: ${videoFile.absolutePath}")
                            }
                            recordButton.text = "녹화 시작"
                        }
                    }
                }
        } catch (e: SecurityException) {
            Log.e("Permission", "권한 없음: ${e.message}")
            requestPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}
