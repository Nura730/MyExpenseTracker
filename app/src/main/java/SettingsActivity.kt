package com.example.myexpensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnTheme).setOnClickListener {
            toggleTheme()
        }

        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            backupData()
        }

        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            restoreBackup()
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            confirmReset()
        }

        findViewById<Button>(R.id.btnCSV).setOnClickListener {
            startActivity(Intent(this, CSVActivity::class.java))
        }

        findViewById<Button>(R.id.btnChangePin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val bioSwitch = findViewById<Switch>(R.id.switchBio)
        val pref = getSharedPreferences("security", MODE_PRIVATE)
        bioSwitch.isChecked = pref.getBoolean("bio", true)

        bioSwitch.setOnCheckedChangeListener { _, checked ->
            toggleBiometric(checked)
        }

        findViewById<Button>(R.id.btnTimer).setOnClickListener {
            showTimerDialog()
        }
    }

    // ---------------- THEME ----------------

    private fun toggleTheme() {
        val pref = getSharedPreferences("ui", MODE_PRIVATE)
        val dark = pref.getBoolean("dark", true)
        pref.edit().putBoolean("dark", !dark).apply()

        Toast.makeText(this,
            "Restart app to apply theme",
            Toast.LENGTH_SHORT).show()
    }

    // ---------------- BACKUP ----------------

    private fun backupData() {

        val data = getSharedPreferences("expense", MODE_PRIVATE)
            .getString("list", "")

        getSharedPreferences("cloud", MODE_PRIVATE)
            .edit().putString("backup", data).apply()

        Toast.makeText(this,
            "Backup saved!",
            Toast.LENGTH_SHORT).show()
    }

    private fun restoreBackup() {

        val cloud = getSharedPreferences("cloud", MODE_PRIVATE)
            .getString("backup", null)

        if (cloud == null) {
            Toast.makeText(this,
                "No backup found",
                Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences("expense", MODE_PRIVATE)
            .edit().putString("list", cloud).apply()

        Toast.makeText(this,
            "Backup restored",
            Toast.LENGTH_SHORT).show()
    }

    // ---------------- RESET ----------------

    private fun confirmReset() {

        AlertDialog.Builder(this)
            .setTitle("Reset App")
            .setMessage("This will delete all data. Continue?")
            .setPositiveButton("YES") { _, _ ->
                resetAll()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetAll() {

        getSharedPreferences("expense", MODE_PRIVATE)
            .edit().clear().apply()

        getSharedPreferences("budget", MODE_PRIVATE)
            .edit().clear().apply()

        Toast.makeText(this,
            "All data cleared!",
            Toast.LENGTH_SHORT).show()
    }

    // ---------------- BIOMETRIC ----------------

    private fun toggleBiometric(enabled: Boolean) {

        getSharedPreferences("security", MODE_PRIVATE)
            .edit().putBoolean("bio", enabled).apply()

        Toast.makeText(this,
            if (enabled) "Fingerprint enabled"
            else "Fingerprint disabled",
            Toast.LENGTH_SHORT).show()
    }

    // ---------------- AUTO LOCK TIMER ----------------

    private fun showTimerDialog() {

        val et = EditText(this)
        et.hint = "Seconds (eg: 30)"
        et.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle("Auto lock timer")
            .setView(et)
            .setPositiveButton("SAVE") { _, _ ->

                val sec = et.text.toString().trim()
                if (sec.isEmpty()) return@setPositiveButton

                getSharedPreferences("security", MODE_PRIVATE)
                    .edit().putInt("timer", sec.toInt()).apply()

                Toast.makeText(this,
                    "Auto lock set to $sec sec",
                    Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
