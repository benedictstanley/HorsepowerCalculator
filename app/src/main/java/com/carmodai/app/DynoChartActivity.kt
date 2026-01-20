package com.carmodai.app

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.pow

import com.carmodai.app.api.DynoDataPoint

import android.widget.Toast

class DynoChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dyno_chart)

        val chart = findViewById<LineChart>(R.id.lineChart)
        setupChart(chart)
        loadChartData(chart)
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setBackgroundColor(Color.parseColor("#1E1E1E"))
        chart.setGridBackgroundColor(Color.parseColor("#1E1E1E"))
        chart.setDrawGridBackground(false)
        chart.isHighlightPerDragEnabled = true

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.parseColor("#40FFFFFF") // White with transparency for better look
        xAxis.setLabelCount(12, true) // More grid lines (forced)

        val leftAxis = chart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#40FFFFFF") // White with transparency
        leftAxis.setLabelCount(12, true) // More grid lines (forced)

        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false

        chart.legend.textColor = Color.WHITE

        // Set Custom Marker View
        val marker = CustomMarkerView(this, R.layout.marker_view)
        marker.chartView = chart
        chart.marker = marker
    }

    private fun loadChartData(chart: LineChart) {
        val hpEntries = ArrayList<Entry>()
        val torqueEntries = ArrayList<Entry>()

        // Get data from Intent
        val dynoData = intent.getSerializableExtra("DYNO_DATA") as? ArrayList<DynoDataPoint>

        if (dynoData != null && dynoData.isNotEmpty()) {
            // Use provided data
            dynoData.forEach { point ->
                hpEntries.add(Entry(point.rpm.toFloat(), point.hp.toFloat()))
                torqueEntries.add(Entry(point.rpm.toFloat(), point.torque.toFloat()))
            }
        } else {
            // Fallback / Demo Mode
            Toast.makeText(this, "No data received. Showing demo chart.", Toast.LENGTH_LONG).show()
            
            val peakTorqueRpm = 6500
            val peakTorque = 350.0
            val maxRpm = intent.getIntExtra("MAX_RPM", 15000)
            
            for (rpm in 2000..maxRpm step 250) {
                 // Simulate a torque curve
                 val dist = (rpm - peakTorqueRpm) / 6000.0
                 val dropOff = dist * dist * 0.5
                 
                 var torque = peakTorque * (1.0 - dropOff)
                 if (torque < peakTorque * 0.4) torque = peakTorque * 0.4
                 
                 val hp = (torque * rpm) / 5252
                 
                 hpEntries.add(Entry(rpm.toFloat(), hp.toFloat()))
                 torqueEntries.add(Entry(rpm.toFloat(), torque.toFloat()))
            }
        }
        
        val hpDataSet = LineDataSet(hpEntries, "HP")
        hpDataSet.color = Color.parseColor("#D32F2F") // Primary Red
        hpDataSet.valueTextColor = Color.WHITE
        hpDataSet.lineWidth = 3f
        hpDataSet.setCircleColor(Color.parseColor("#D32F2F"))
        hpDataSet.circleRadius = 4f
        hpDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        hpDataSet.setDrawValues(false)
        hpDataSet.setDrawCircles(true)
        hpDataSet.setDrawCircleHole(false)

        val torqueDataSet = LineDataSet(torqueEntries, "Torque (ft-lbs)")
        torqueDataSet.color = Color.parseColor("#4CAF50") // Green
        torqueDataSet.valueTextColor = Color.WHITE
        torqueDataSet.lineWidth = 3f
        torqueDataSet.setCircleColor(Color.parseColor("#4CAF50"))
        torqueDataSet.circleRadius = 4f
        torqueDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        torqueDataSet.setDrawValues(false)
        torqueDataSet.setDrawCircles(true)
        torqueDataSet.setDrawCircleHole(false)
        
        val data = LineData(hpDataSet, torqueDataSet)
        chart.data = data
        chart.invalidate() // Refresh
    }
}
