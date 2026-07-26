package com.tiffany.symptomchecker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tiffany.symptomchecker.R
import com.tiffany.symptomchecker.data.ProfileStore

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tvWelcome)
        val btnStart = findViewById<Button>(R.id.btnStartChecker)
        val btnProfile = findViewById<Button>(R.id.btnProfile)
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnAbout = findViewById<Button>(R.id.btnAbout)

        btnStart.setOnClickListener { startActivity(Intent(this, SymptomSelectionActivity::class.java)) }
        btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        btnAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About Symptom Checker")
                .setMessage(
                    "This app helps users review symptoms, receive general care guidance, find nearby care and save checks locally.\n\n" +
                        "It does not diagnose disease or replace a qualified healthcare professional.\n\n" +
                        "In a life-threatening emergency in Malaysia, call 999."
                )
                .setPositiveButton("OK", null)
                .show()
        }

        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (!prefs.getBoolean("disclaimer_seen", false)) {
            AlertDialog.Builder(this)
                .setTitle("Important medical disclaimer")
                .setMessage(
                    "This app provides informational guidance only. It does not provide a medical diagnosis or treatment. " +
                        "Seek professional care when symptoms are severe, persistent or worsening. Call 999 for emergencies."
                )
                .setPositiveButton("I understand") { _, _ ->
                    prefs.edit().putBoolean("disclaimer_seen", true).apply()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        val profile = ProfileStore.load(this)
        tvWelcome.text = if (profile.name.isBlank()) {
            "How are you feeling today?"
        } else {
            "Hi ${profile.name}, how are you feeling today?"
        }
    }
}
