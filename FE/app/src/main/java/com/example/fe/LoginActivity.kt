package com.example.fe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView    // ▶ 추가된 import
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.FormBody
import org.json.JSONObject      // ▶ 추가된 import
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    companion object {
        // ▶ App.kt에서 만든 싱글턴 OkHttpClient 사용
        private val client: OkHttpClient = App.httpClient
        private const val BASE_URL = "http://homept.online:8080"
    }

    private lateinit var etId: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView  // ▶ TextView로 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etId       = findViewById(R.id.etId)
        etPassword = findViewById(R.id.etPassword)
        btnLogin   = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)  // ▶ TextView로 바꿔 찾기

        btnLogin.setOnClickListener {
            val id  = etId.text.toString().trim()
            val pwd = etPassword.text.toString().trim()
            if (id.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            } else {
                doLogin(id, pwd)
            }
        }

        tvRegister.setOnClickListener {
            // 회원가입 화면으로 이동
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin(id: String, pwd: String) {
        // ▶ 로그인 요청 (폼 바디)
        val formBody = FormBody.Builder()
            .add("id", id)
            .add("password", pwd)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/api/user/login")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "로그인 요청 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string().orEmpty()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                        // ▶ 로그인 성공 후 내 정보(uid) 요청
                        val meReq = Request.Builder()
                            .url("$BASE_URL/api/user/me")
                            .get()
                            .build()

                        client.newCall(meReq).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                // 실패해도 계속 진행
                            }
                            override fun onResponse(call: Call, res: Response) {
                                if (res.isSuccessful) {
                                    val json = JSONObject(res.body!!.string())
                                    val uid  = json.getInt("uid")
                                    // ▶ SharedPreferences에 uid 저장
                                    getSharedPreferences("auth", MODE_PRIVATE)
                                        .edit()
                                        .putInt("uid", uid)
                                        .apply()
                                }
                            }
                        })

                        // 홈 화면으로 이동
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "로그인 실패: $bodyStr",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
