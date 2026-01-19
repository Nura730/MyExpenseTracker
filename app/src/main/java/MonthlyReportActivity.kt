package com.example.myexpensetracker

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import java.text.SimpleDateFormat
import java.util.*

class MonthlyReportActivity : AppCompatActivity() {

    lateinit var barChart: BarChart
    lateinit var tvTitle: TextView
    lateinit var btnExport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly)

        barChart = findViewById(R.id.barChart)
        tvTitle = findViewById(R.id.tvMonth)
        btnExport = findViewById(R.id.btnExport)

        generateMonthlyReport()

        btnExport.setOnClickListener {
            exportPDF()
        }
    }

    private fun generateMonthlyReport() {

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        val raw = pref.getString("list", "") ?: ""

        val map = HashMap<Int, Float>()   // day -> total

        val cal = Calendar.getInstance()
        val nowMonth = cal.get(Calendar.MONTH)
        val nowYear = cal.get(Calendar.YEAR)

        for (line in raw.split("\n")) {

            if (line.isBlank()) continue

            val p = line.split("|")
            if (p.size < 4) continue

            val amount = p[0].toFloatOrNull() ?: 0f
            val time = p[3].toLong()

            cal.timeInMillis = time

            if (
                cal.get(Calendar.MONTH) == nowMonth &&
                cal.get(Calendar.YEAR) == nowYear
            ) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: 0f) + amount
            }
        }

        // Title
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvTitle.text = sdf.format(Date())

        // Chart
        val entries = ArrayList<BarEntry>()

        for ((day, value) in map) {
            entries.add(BarEntry(day.toFloat(), value))
        }

        val dataSet = BarDataSet(entries, "Daily Expense")
        dataSet.color = Color.CYAN
        dataSet.valueTextColor = Color.WHITE

        barChart.apply {
            description.isEnabled = false
            setFitBars(true)
            data = BarData(dataSet)
            invalidate()
        }
    }

    private fun exportPDF() {
        Toast.makeText(this,"PDF export coming next step",Toast.LENGTH_SHORT).show()
    }
}
