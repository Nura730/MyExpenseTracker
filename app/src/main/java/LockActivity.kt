package com.example.myexpensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LockActivity : AppCompatActivity() {

    lateinit var executor: Executor
    lateinit var biometricPrompt: BiometricPrompt
    lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        val pref = getSharedPreferences("security", MODE_PRIVATE)
        val savedPin = pref.getString("pin", null)
        val last = pref.getLong("last", 0)
        val timer = pref.getInt("timer",30)

        // AUTO UNLOCK
        if(System.currentTimeMillis() - last < timer * 1000){
            goHome()
            return
        }

        // 🔐 BIOMETRIC
        val bioEnabled = pref.getBoolean("bio", true)
        if(bioEnabled){
            checkBiometric()
        }

        // FIRST TIME
        if(savedPin == null){
            Toast.makeText(this,"Create new PIN",Toast.LENGTH_SHORT).show()
        }

        btnUnlock.setOnClickListener {

            val input = etPin.text.toString()

            if(input.length != 4){
                Toast.makeText(this,"Enter 4 digit PIN",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(savedPin == null){
                pref.edit().putString("pin", input).apply()
                goHome()
            }else{
                if(input == savedPin){
                    goHome()
                }else{
                    Toast.makeText(this,"Wrong PIN",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goHome(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ---------------- BIOMETRIC ----------------

    private fun checkBiometric(){

        val biometricManager = BiometricManager.from(this)

        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if(result == BiometricManager.BIOMETRIC_SUCCESS){
            showBiometricPrompt()
        }
        else{
            Toast.makeText(this,
                "Fingerprint not set on device",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBiometricPrompt(){

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback(){

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ){
                    super.onAuthenticationSucceeded(result)
                    goHome()
                }

                override fun onAuthenticationFailed(){
                    Toast.makeText(
                        this@LockActivity,
                        "Fingerprint not recognized",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MyExpenseTracker")
            .setSubtitle("Use your fingerprint")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
