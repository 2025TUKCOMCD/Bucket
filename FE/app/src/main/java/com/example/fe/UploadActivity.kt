package com.example.fe

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*

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

        // 1) MainActivity 에서 넘어온 종목 세팅
        val presetSport = intent.getStringExtra("sportname")
        presetSport?.let {
            etSport.setText(it)
        }

        // 2) etDate 클릭 시 DatePickerDialog 띄우기
        etDate.setOnClickListener {
            showDatePicker()
        }

        btnUpload.setOnClickListener {
            val sport    = presetSport ?: etSport.text.toString().trim()
            val date     = etDate.text.toString().trim()
            val url      = etUrl.text.toString().trim()
            val feedback = etFeedback.text.toString().trim()
            val uid      = getSharedPreferences("auth", MODE_PRIVATE)
                .getInt("uid", 1)

            if (sport.isEmpty() || date.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "운동, 날짜, URL은 필수 입력입니다.", Toast.LENGTH_SHORT).show()
            } else {
                uploadRecord(sport, date, url, feedback, uid)
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        // 기본값: 오늘 날짜
        val year  = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day   = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            // 선택 결과를 "yyyy-MM-dd" 형식으로 EditText 에 표시
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            cal.set(y, m, d)
            etDate.setText(sdf.format(cal.time))
        }, year, month, day).show()
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
                        Toast.makeText(this@UploadActivity, "업로드 성공!", Toast.LENGTH_SHORT).show()
                        finish()
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
