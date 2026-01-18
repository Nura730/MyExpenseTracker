package com.example.myexpensetracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class CSVActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportCSV()
    }

    private fun exportCSV(){

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        val data = pref.getString("list","") ?: ""

        if(data.isBlank()){
            Toast.makeText(this,"No data to export",Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "expenses.csv"
        )

        val builder = StringBuilder()
        builder.append("Amount,Note,Category,Date\n")

        val lines = data.split("\n")

        for(line in lines){

            if(line.isBlank()) continue

            val p = line.split("|")
            if(p.size < 4) continue

            val date = java.text.SimpleDateFormat(
                "dd-MM-yyyy",
                java.util.Locale.getDefault()
            ).format(p[3].toLong())

            builder.append(
                "${p[0]},${p[1]},${p[2]},$date\n"
            )
        }

        FileOutputStream(file).use {
            it.write(builder.toString().toByteArray())
        }

        Toast.makeText(this,
            "CSV saved:\n${file.absolutePath}",
            Toast.LENGTH_LONG).show()

        shareFile(file)
    }

    // ---------------- SHARE ----------------

    private fun shareFile(file: File){

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intent,"Share CSV"))
        finish()
    }
}
