// App.kt
package com.example.fe

import android.app.Application
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.CookiePolicy

class App : Application() {
    companion object {
        // 앱 전체에서 공유할 OkHttpClient
        lateinit var httpClient: OkHttpClient
    }

    override fun onCreate() {
        super.onCreate()
        // 쿠키 매니저 설정 (세션 쿠키 자동 저장/전달)
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        httpClient = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()
    }
}
