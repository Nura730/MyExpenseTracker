package com.example.myexpensetracker

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private var reportText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            createPDF()
        }

        loadReport()
    }

    private fun loadReport(){

        val tvTotal = findViewById<TextView>(R.id.tvMonthTotal)
        val tvMax = findViewById<TextView>(R.id.tvMaxExpense)
        val tvCategory = findViewById<TextView>(R.id.tvTopCategory)
        val tvAvg = findViewById<TextView>(R.id.tvAvg)

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        val data = pref.getString("list","") ?: ""

        if(data.isBlank()){
            Toast.makeText(this,"No data for report",Toast.LENGTH_SHORT).show()
            return
        }

        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        val year = now.get(Calendar.YEAR)

        var total = 0f
        var max = 0f
        val map = HashMap<String,Float>()
        val days = HashSet<Int>()

        val lines = data.split("\n")

        for(line in lines){

            if(line.isBlank()) continue

            val p = line.split("|")
            if(p.size < 4) continue

            val amount = p[0].toFloatOrNull() ?: 0f
            val category = p[2]
            val time = p[3].toLong()

            val cal = Calendar.getInstance()
            cal.timeInMillis = time

            if(
                cal.get(Calendar.MONTH) != month ||
                cal.get(Calendar.YEAR) != year
            ) continue

            total += amount
            if(amount > max) max = amount

            map[category] = (map[category] ?: 0f) + amount
            days.add(cal.get(Calendar.DAY_OF_MONTH))
        }

        val topCat = map.maxByOrNull { it.value }?.key ?: "N/A"
        val avg = if(days.isEmpty()) 0 else total / days.size

        tvTotal.text = "This Month Total: ₹$total"
        tvMax.text = "Highest Expense: ₹$max"
        tvCategory.text = "Top Category: $topCat"
        tvAvg.text = "Avg per day: ₹$avg"

        reportText = """
MONTHLY EXPENSE REPORT

Month: ${SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date())}

Total: ₹$total
Highest Expense: ₹$max
Top Category: $topCat
Avg Per Day: ₹$avg
""".trimIndent()
    }

    // ---------------- PDF ----------------

    private fun createPDF(){

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo
            .Builder(400, 700, 1).create()

        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        paint.textSize = 14f

        var y = 40
        for(line in reportText.split("\n")){
            canvas.drawText(line, 20f, y.toFloat(), paint)
            y += 25
        }

        doc.finishPage(page)

        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "ExpenseReport_${System.currentTimeMillis()}.pdf"
        )

        FileOutputStream(file).use {
            doc.writeTo(it)
        }

        doc.close()

        Toast.makeText(
            this,
            "PDF saved:\n${file.absolutePath}",
            Toast.LENGTH_LONG
        ).show()

        sharePDF(file)
    }

    // ---------------- SHARE ----------------

    private fun sharePDF(file: File){

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(
            Intent.createChooser(intent,"Share Report")
        )
    }


}
