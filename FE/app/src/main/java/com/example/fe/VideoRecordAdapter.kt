package com.example.fe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fe.R
import com.example.fe.model.VideoRecord

class VideoRecordAdapter(
    private var items: List<VideoRecord>
) : RecyclerView.Adapter<VideoRecordAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSport    = view.findViewById<TextView>(R.id.tvSport)
        val tvDate     = view.findViewById<TextView>(R.id.tvDate)
        val tvFeedback = view.findViewById<TextView>(R.id.tvFeedback)
        val tvUrl      = view.findViewById<TextView>(R.id.tvUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_record, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvSport.text    = item.sportname
        holder.tvDate.text     = item.date
        holder.tvFeedback.text = if (item.feedback.isNotEmpty()) item.feedback else "피드백 없음"
        holder.tvUrl.text      = item.videoUrl
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<VideoRecord>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
