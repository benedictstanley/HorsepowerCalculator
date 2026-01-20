package com.carmodai.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class DynoGraphView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#1E1E1E"))
        
        // Draw axis
        canvas.drawLine(50f, height - 50f, width - 50f, height - 50f, paint) // X
        canvas.drawLine(50f, height - 50f, 50f, 50f, paint) // Y
        
        // Draw dummy curve
        canvas.drawLine(50f, height - 50f, width - 50f, 50f, paint)
        
        canvas.drawText("RPM", width - 100f, height - 20f, textPaint)
        canvas.drawText("HP", 10f, 40f, textPaint)
    }
}