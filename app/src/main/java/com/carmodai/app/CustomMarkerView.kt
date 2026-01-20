package com.carmodai.app

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        // Format: RPM: 1234, Val: 123
        tvContent.text = String.format("RPM: %.0f\nVal: %.0f", e.x, e.y)

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center the marker horizontally and position it slightly above the point
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
