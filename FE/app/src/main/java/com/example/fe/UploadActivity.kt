// UploadActivity.kt
package com.example.fe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.RequestBody
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

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
        setContentView(R.layout.activity_upload)

        etSport    = findViewById(R.id.etSport)
        etDate     = findViewById(R.id.etDate)
        etUrl      = findViewById(R.id.etUrl)
        etFeedback = findViewById(R.id.etFeedback)
        btnUpload  = findViewById(R.id.btnUpload)

        // ❶ MainActivity에서 전달한 종목
        val presetSport = intent.getStringExtra("sportname")
        if (!presetSport.isNullOrEmpty()) {
            etSport.setText(presetSport)
            etSport.isEnabled = false
        }

        btnUpload.setOnClickListener {
            val sport    = presetSport ?: etSport.text.toString().trim()
            val date     = etDate.text.toString().trim()
            val url      = etUrl.text.toString().trim()
            val feedback = etFeedback.text.toString().trim()
            val uid      = getSharedPreferences("auth", MODE_PRIVATE)
                .getInt("uid", 1)

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

        App.httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@UploadActivity,
                        "업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UploadActivity, "업로드 성공!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@UploadActivity,
                            "업로드 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
