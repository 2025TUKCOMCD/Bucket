    package com.example.fe

    import android.os.Bundle
    import android.widget.CalendarView
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.fe.adapter.VideoRecordAdapter
    import com.example.fe.model.VideoRecord
    import okhttp3.*
    import org.json.JSONArray
    import java.io.IOException
    import java.text.SimpleDateFormat
    import java.util.*

    class HistoryActivity : AppCompatActivity() {

        private lateinit var calendarView: CalendarView
        private lateinit var tvSelectedDate: TextView
        private lateinit var rvHistory: RecyclerView
        private lateinit var allRecords: List<VideoRecord>
        private lateinit var adapter: VideoRecordAdapter

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_history)

            calendarView   = findViewById(R.id.calendarView)
            tvSelectedDate = findViewById(R.id.tvSelectedDate)
            rvHistory      = findViewById(R.id.rvHistory)

            rvHistory.layoutManager = LinearLayoutManager(this)
            adapter = VideoRecordAdapter(emptyList())
            rvHistory.adapter = adapter

            // 전체 기록 로드
            loadHistory { list ->
                runOnUiThread {
                    if (list != null) {
                        allRecords = list
                        // 오늘 날짜 필터링
                        updateForDate(Calendar.getInstance().timeInMillis)
                    } else {
                        Toast.makeText(this, "기록 조회 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // 달력 날짜 선택 시
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                updateForDate(cal.timeInMillis)
            }
        }

        private fun updateForDate(timeInMillis: Long) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(Date(timeInMillis))
            tvSelectedDate.text = getString(R.string.selected_date, dateStr)

            // 클라이언트 필터링
            val filtered = allRecords.filter { it.date == dateStr }
            adapter.updateItems(filtered)
        }

        private fun loadHistory(callback: (List<VideoRecord>?) -> Unit) {
            val request = Request.Builder()
                .url("$BASE_URL/api/user-videos/my")
                .get()
                .build()

            App.httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(null)
                }
                override fun onResponse(call: Call, res: Response) {
                    if (!res.isSuccessful) {
                        callback(null)
                        return
                    }
                    val body = res.body?.string().orEmpty()
                    val arr  = JSONArray(body)
                    val list = mutableListOf<VideoRecord>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += VideoRecord(
                            vid       = o.getInt("vid"),
                            sportname = o.getString("sportname"),
                            date      = o.getString("date"),
                            feedback  = o.getString("feedback"),
                            videoUrl  = o.getString("videoUrl")
                        )
                    }
                    callback(list)
                }
            })
        }

        companion object {
            private const val BASE_URL = "http://3.39.137.250:8080"
        }
    }
