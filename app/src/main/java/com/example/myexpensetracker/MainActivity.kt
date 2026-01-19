package com.example.myexpensetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var spinner: Spinner
    lateinit var listView: ListView
    lateinit var tvTotal: TextView
    lateinit var tvBudget: TextView
    lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        spinner = findViewById(R.id.spFilter)
        listView = findViewById(R.id.listView)
        tvTotal = findViewById(R.id.tvTotal)
        tvBudget = findViewById(R.id.tvBudget)
        pieChart = findViewById(R.id.pieChart)

        val btnSetBudget = findViewById<Button>(R.id.btnSetBudget)

        // Bottom Nav
        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navAdd = findViewById<ImageButton>(R.id.navAdd)
        val navReport = findViewById<ImageButton>(R.id.navReport)
        val navSettings = findViewById<ImageButton>(R.id.navSettings)

        setupFilterSpinner()

        // NAV ACTIONS
        navHome.setOnClickListener {
            loadData()
            Toast.makeText(this,"Home",Toast.LENGTH_SHORT).show()
        }

        navAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        navReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSetBudget.setOnClickListener {
            showBudgetDialog()
        }

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    loadData()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // ---------------- FILTER ----------------

    private fun setupFilterSpinner() {

        val filters = arrayOf(
            "All",
            "Today",
            "Yesterday",
            "This Week",
            "This Month"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filters
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter
    }

    // ---------------- LOAD DATA ----------------

    private fun loadData() {

        val pref = getSharedPreferences("expense", MODE_PRIVATE)
        val data = pref.getString("list", "") ?: ""

        val now = System.currentTimeMillis()

        var total = 0f
        val items = mutableListOf<String>()
        val rawLines = mutableListOf<String>()

        val filter = spinner.selectedItem?.toString() ?: "All"

        for (line in data.split("\n")) {

            if (line.isBlank()) continue

            val parts = line.split("|")
            if (parts.size < 4) continue

            val amount = parts[0].toFloatOrNull() ?: 0f
            val note = parts[1]
            val category = parts[2]
            val time = parts[3].toLong()

            if (!matchFilter(filter, time, now)) continue

            total += amount
            items.add("₹$amount - $note ($category)")
            rawLines.add(line)
        }

        // TOTAL
        tvTotal.text = "₹$total"

        // BUDGET
        val budgetPref = getSharedPreferences("budget", MODE_PRIVATE)
        val limitStr = budgetPref.getString("limit", null)

        if (limitStr != null) {

            val limit = limitStr.toFloat()
            val remaining = limit - total

            tvBudget.text = "Budget: ₹$limit | Left: ₹$remaining"

            when {
                remaining <= 0 ->
                    tvBudget.setTextColor(android.graphics.Color.RED)
                remaining < limit * 0.2 ->
                    tvBudget.setTextColor(android.graphics.Color.YELLOW)
                else ->
                    tvBudget.setTextColor(android.graphics.Color.GREEN)
            }

        } else {
            tvBudget.text = "Budget: Not set"
            tvBudget.setTextColor(android.graphics.Color.WHITE)
        }

        // LIST
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            items
        )

        listView.setOnItemClickListener { _, _, pos, _ ->
            showEditDialog(pos, rawLines)
        }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            deleteItem(pos, rawLines)
            true
        }

        loadPieChart()
    }

    // ---------------- FILTER LOGIC ----------------

    private fun matchFilter(type: String, time: Long, now: Long): Boolean {

        val cal = Calendar.getInstance()

        return when (type) {

            "Today" -> {
                cal.timeInMillis = now
                val d1 = cal.get(Calendar.DAY_OF_YEAR)
                cal.timeInMillis = time
                val d2 = cal.get(Calendar.DAY_OF_YEAR)
                d1 == d2
            }

            "Yesterday" -> {
                cal.timeInMillis = now
                val d1 = cal.get(Calendar.DAY_OF_YEAR) - 1
                cal.timeInMillis = time
                val d2 = cal.get(Calendar.DAY_OF_YEAR)
                d1 == d2
            }

            "This Week" -> {
                cal.timeInMillis = now
                val w1 = cal.get(Calendar.WEEK_OF_YEAR)
                cal.timeInMillis = time
                val w2 = cal.get(Calendar.WEEK_OF_YEAR)
                w1 == w2
            }

            "This Month" -> {
                cal.timeInMillis = now
                val m1 = cal.get(Calendar.MONTH)
                cal.timeInMillis = time
                val m2 = cal.get(Calendar.MONTH)
                m1 == m2
            }

            else -> true
        }
    }

    // ---------------- EDIT ----------------

    private fun showEditDialog(
        position: Int,
        rawLines: MutableList<String>
    ) {

        val parts = rawLines[position].split("|")

        val et = EditText(this)
        et.setText(parts[0])
        et.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle("Edit Amount")
            .setView(et)
            .setPositiveButton("SAVE") { _, _ ->

                val newAmount = et.text.toString().trim()
                if (newAmount.isEmpty()) return@setPositiveButton

                val newLine =
                    "$newAmount|${parts[1]}|${parts[2]}|${parts[3]}"

                rawLines[position] = newLine

                val pref =
                    getSharedPreferences("expense", MODE_PRIVATE)

                pref.edit()
                    .putString(
                        "list",
                        rawLines.joinToString("\n") + "\n"
                    )
                    .apply()

                loadData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------- DELETE ----------------

    private fun deleteItem(
        position: Int,
        rawLines: MutableList<String>
    ) {

        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete this expense?")
            .setPositiveButton("DELETE") { _, _ ->

                rawLines.removeAt(position)

                val pref =
                    getSharedPreferences("expense", MODE_PRIVATE)

                pref.edit()
                    .putString(
                        "list",
                        rawLines.joinToString("\n") + "\n"
                    )
                    .apply()

                Toast.makeText(
                    this,
                    "Deleted",
                    Toast.LENGTH_SHORT
                ).show()

                loadData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------- PIE ----------------

    private fun loadPieChart() {

        val pref =
            getSharedPreferences("expense", MODE_PRIVATE)

        val rawData =
            pref.getString("list", "") ?: ""

        val map =
            HashMap<String, Float>()

        for (line in rawData.split("\n")) {

            if (line.isBlank()) continue

            val parts = line.split("|")
            if (parts.size < 3) continue

            val amount =
                parts[0].toFloatOrNull() ?: 0f

            val category = parts[2]

            map[category] =
                (map[category] ?: 0f) + amount
        }

        val entries =
            ArrayList<PieEntry>()

        for ((key, value) in map) {
            entries.add(PieEntry(value, key))
        }

        val dataSet =
            PieDataSet(entries, "")

        dataSet.colors = listOf(
            0xFF00E5FF.toInt(),
            0xFF7C4DFF.toInt(),
            0xFFFF5252.toInt(),
            0xFF69F0AE.toInt(),
            0xFFFFD740.toInt()
        )

        dataSet.valueTextColor =
            android.graphics.Color.WHITE

        pieChart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 60f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            legend.isEnabled = false
            this.data = PieData(dataSet)   // ✔ correct
            invalidate()
        }
    }


    // ---------------- BUDGET ----------------

    private fun showBudgetDialog() {

        val et = EditText(this)
        et.hint = "Enter monthly budget"
        et.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle("Set Budget")
            .setView(et)
            .setPositiveButton("SAVE") { _, _ ->

                val value =
                    et.text.toString().trim()

                if (value.isEmpty())
                    return@setPositiveButton

                val pref =
                    getSharedPreferences("budget", MODE_PRIVATE)

                pref.edit()
                    .putString("limit", value)
                    .apply()

                loadData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------- AUTO LOCK ----------------

    override fun onPause() {
        super.onPause()
        getSharedPreferences("security", MODE_PRIVATE)
            .edit()
            .putLong(
                "last",
                System.currentTimeMillis()
            )
            .apply()
    }
}
