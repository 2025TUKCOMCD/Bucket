package com.example.fe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

// NormalizedLandmark (0.10.x)
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var landmarks: List<NormalizedLandmark> = emptyList()

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>) {
        landmarks = newLandmarks
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        landmarks.forEach { lm ->
            val x = lm.x() * width
            val y = lm.y() * height
            canvas.drawCircle(x, y, 10f, paint)
        }
    }
}