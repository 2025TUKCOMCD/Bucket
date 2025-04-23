// app/src/main/java/com/example/fe/UploadActivity.kt
package com.example.fe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException

class UploadActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "http://3.39.137.250:8080"
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    }

    private lateinit var etSport: EditText
    private lateinit var etDate: EditText
    private lateinit var etUrl: EditText
    private lateinit var etFeedback: EditText
    private lateinit var btnUpload: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 간단한 레이아웃이 필요합니다. 예: res/layout/activity_upload.xml
        setContentView(R.layout.activity_upload)

        etSport     = findViewById(R.id.etSport)
        etDate      = findViewById(R.id.etDate)
        etUrl       = findViewById(R.id.etUrl)
        etFeedback  = findViewById(R.id.etFeedback)
        btnUpload   = findViewById(R.id.btnUpload)

        btnUpload.setOnClickListener {
            val sport    = etSport.text.toString().trim()
            val date     = etDate.text.toString().trim()
            val url      = etUrl.text.toString().trim()
            val feedback = etFeedback.text.toString().trim()
            // TODO: 실제 로그인된 유저의 uid 로 바꿔주세요
            val uid      = 1

            uploadRecord(sport, date, url, feedback, uid)
        }
    }

    private fun uploadRecord(
        sportname: String,
        recordDate: String,
        videoUrl: String,
        feedback: String,
        uid: Int
    ) {
        // JSON 생성
        val json = JSONObject().apply {
            put("sportname", sportname)
            put("recordDate", recordDate)
            put("videoUrl", videoUrl)
            put("feedback", feedback)
            put("user", JSONObject().put("uid", uid))
        }.toString()

        val body = RequestBody.create(JSON, json)
        val request = Request.Builder()
            .url("$BASE_URL/api/user-videos")
            .post(body)
            .build()

        // App.httpClient 는 App.kt 에서 만든 공유 클라이언트
        App.httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@UploadActivity,
                        "업로드 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@UploadActivity,
                            "업로드 성공!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@UploadActivity,
                            "업로드 실패: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
