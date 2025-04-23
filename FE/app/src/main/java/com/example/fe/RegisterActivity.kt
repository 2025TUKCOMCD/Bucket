package com.example.fe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "http://3.39.137.250:8080"
    }

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etRegId: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var btnRegister: Button

    private val client: OkHttpClient by lazy {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etBirthday = findViewById(R.id.etBirthday)
        etRegId = findViewById(R.id.etRegId)
        etRegPassword = findViewById(R.id.etRegPassword)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val rawBirth = etBirthday.text.toString().trim()
            val id = etRegId.text.toString().trim()
            val pwd = etRegPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() ||
                rawBirth.isEmpty() || id.isEmpty() || pwd.isEmpty()
            ) {
                Toast.makeText(
                    this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT
                ).show()
            } else {
                // rawBirth: "20000517" → formattedBirth: "2000-05-17"
                val formattedBirth = rawBirth
                    .replace(Regex("""(\d{4})(\d{2})(\d{2})"""), "$1-$2-$3")

                registerUser(username, email, formattedBirth, id, pwd)
            }
        }
    }

    private fun registerUser(
        username: String,
        email: String,
        birthday: String,
        id: String,
        pwd: String
    ) {
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("birthday", birthday)
            put("id", id)
            put("pwd", pwd)
        }.toString()

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json
        )

        val request = Request.Builder()
            .url("$BASE_URL/api/user/register")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity,
                        getString(R.string.register_request_fail, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.register_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity,
                            getString(R.string.register_fail, body),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
