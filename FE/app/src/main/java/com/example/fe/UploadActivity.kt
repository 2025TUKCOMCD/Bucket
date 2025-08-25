package com.example.fe

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.FormBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UploadActivity : AppCompatActivity() {

    companion object {
        // HTTPS 기본 포트(443) 로 서비스 중이라면 포트번호 생략
        private const val BASE_URL = "https://homept.online"
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

        // MainActivity에서 전달된 종목 설정
        intent.getStringExtra("sportname")?.also { etSport.setText(it) }

        // 날짜 선택
        etDate.setOnClickListener { showDatePicker() }

        btnUpload.setOnClickListener {
            val uid      = getSharedPreferences("auth", MODE_PRIVATE).getInt("uid", 1)
            val sport    = etSport.text.toString().trim()
            val date     = etDate.text.toString().trim()
            val feedback = etFeedback.text.toString().trim()

            if (sport.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "운동명과 날짜는 필수입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ① Redis에서 S3 URL 꺼내오기
            fetchVideoUrl(uid) { s3Url ->
                // UI에 미리보기용으로 넣어두고
                runOnUiThread { etUrl.setText(s3Url) }
                // ② 그 URL을 포함해 기록 저장
                postRecord(uid, sport, date, s3Url, feedback)
            }
        }
    }

    private fun fetchVideoUrl(uid: Int, onResult: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/user-videos/url/$uid")
            .get()
            .build()

        App.httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 조회 실패해도 그냥 빈 값으로 업로드 진행
                runOnUiThread {
                    Toast.makeText(this@UploadActivity, "URL 조회 실패: ${e.message}, 빈 값으로 업로드합니다.", Toast.LENGTH_SHORT).show()
                }
                onResult("") // 🚩 빈 값 전달
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim().orEmpty()
                    onResult(body) // 비어있으면 그냥 빈 값 전달
                } else {
                    runOnUiThread {
                        Toast.makeText(this@UploadActivity, "URL 조회 오류(${response.code}), 빈 값으로 업로드합니다.", Toast.LENGTH_SHORT).show()
                    }
                    onResult("") // 🚩 빈 값 전달
                }
            }
        })
    }

    private fun postRecord(
        uid: Int,
        sport: String,
        recordDate: String,
        videoUrl: String,
        feedback: String
    ) {
        val form = FormBody.Builder()
            .add("uid", uid.toString())
            .add("sportname", sport)
            .add("recordDate", recordDate)
            .add("videoUrl", videoUrl)
            .add("feedback", feedback)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/api/user-videos")
            .post(form)
            .build()

        App.httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@UploadActivity, "기록 업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UploadActivity, "기록 업로드 성공!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@UploadActivity,
                            "업로드 실패(${response.code}): $body",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this,
            { _, y, m, d ->
                cal.set(y, m, d)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etDate.setText(sdf.format(cal.time))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
