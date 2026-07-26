package com.tiffany.symptomchecker.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.tiffany.symptomchecker.R
import com.tiffany.symptomchecker.data.HistoryStore
import com.tiffany.symptomchecker.data.ProfileStore
import com.tiffany.symptomchecker.model.CareGuidanceEngine
import com.tiffany.symptomchecker.model.PredictResponse

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        supportActionBar?.title = "Results"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val json = intent.getStringExtra("RESULT_JSON") ?: return
        val result = Gson().fromJson(json, PredictResponse::class.java)
        val severity = intent.getStringExtra("SEVERITY") ?: "Not specified"
        val duration = intent.getStringExtra("DURATION") ?: "Not specified"
        val guidance = CareGuidanceEngine.evaluate(result.symptoms_analysed, severity, duration)
        HistoryStore.save(this, result, severity, duration, guidance.level)

        findViewById<TextView>(R.id.tvCheckDetails).text =
            "Severity: $severity\nDuration: $duration\nSymptoms selected: ${result.symptoms_analysed.size}"

        val cardTriage = findViewById<CardView>(R.id.cardTriage)
        cardTriage.setCardBackgroundColor(Color.parseColor(guidance.color))
        findViewById<TextView>(R.id.tvTriageIcon).text = guidance.icon
        findViewById<TextView>(R.id.tvTriageLevel).text = guidance.level
        findViewById<TextView>(R.id.tvTriageMessage).text = guidance.message
        findViewById<TextView>(R.id.tvNextStep).text = guidance.nextStep

        val tvRedFlag = findViewById<TextView>(R.id.tvRedFlag)
        tvRedFlag.visibility = if (guidance.isRedFlag) View.VISIBLE else View.GONE
        if (guidance.isRedFlag) {
            tvRedFlag.text = "Potential urgent warning: do not rely on this app if the person is severely unwell. Seek immediate professional help."
        }

        renderPredictions(result)
        val symptoms = result.symptoms_analysed.joinToString(", ") { it.replace("_", " ") }
        findViewById<TextView>(R.id.tvAnalysedSymptoms).text = "Symptoms analysed: $symptoms"
        findViewById<TextView>(R.id.tvDisclaimer).text =
            "Informational use only. The model matches selected symptoms to possible conditions. Care guidance is general and does not replace medical assessment."

        val btnCall999 = findViewById<Button>(R.id.btnCall999)
        btnCall999.visibility = if (guidance.level == "Emergency") View.VISIBLE else View.GONE
        btnCall999.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Call emergency services?")
                .setMessage("This will open your phone dialler with Malaysia emergency number 999. Only call for a genuine emergency.")
                .setPositiveButton("Open dialler") { _, _ -> dial("999") }
                .setNegativeButton("Cancel", null)
                .show()
        }
        findViewById<Button>(R.id.btnNearbyClinic).setOnClickListener { openNearby("clinic near me") }
        findViewById<Button>(R.id.btnNearbyHospital).setOnClickListener { openNearby("hospital near me") }
        findViewById<Button>(R.id.btnPreferredHospital).setOnClickListener {
            val profile = ProfileStore.load(this)
            if (profile.preferredHospitalPhone.isBlank()) {
                AlertDialog.Builder(this)
                    .setTitle("No preferred hospital saved")
                    .setMessage("Add a hospital name and phone number in Health Profile first.")
                    .setPositiveButton("Open profile") { _, _ -> startActivity(Intent(this, ProfileActivity::class.java)) }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                val label = profile.preferredHospital.ifBlank { "preferred hospital" }
                AlertDialog.Builder(this)
                    .setTitle("Call $label?")
                    .setMessage(profile.preferredHospitalPhone)
                    .setPositiveButton("Open dialler") { _, _ -> dial(profile.preferredHospitalPhone) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        findViewById<Button>(R.id.btnNewCheck).setOnClickListener { finish() }
    }

    private fun renderPredictions(result: PredictResponse) {
        val container = findViewById<LinearLayout>(R.id.predictionsContainer)
        val rankColors = listOf("#184E77", "#1D6A8A", "#2B7A9B")
        result.top3_predictions.forEachIndexed { index, prediction ->
            val card = layoutInflater.inflate(R.layout.item_prediction, container, false)
            card.findViewById<TextView>(R.id.tvRank).text = "#${index + 1}"
            card.findViewById<TextView>(R.id.tvDisease).text = prediction.disease
            card.findViewById<TextView>(R.id.tvConfidence).text = "${prediction.confidence}%"
            card.findViewById<TextView>(R.id.tvTriage).apply {
                text = "Possible model match"
                setTextColor(Color.parseColor("#DCEAF7"))
            }
            card.findViewById<ProgressBar>(R.id.pbConfidence).progress = prediction.confidence.coerceIn(0.0, 100.0).toInt()
            card.setBackgroundColor(Color.parseColor(rankColors[index]))
            container.addView(card)
        }
    }

    private fun dial(number: String) {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) {
            Toast.makeText(this, "No valid phone number saved", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
    }

    private fun openNearby(query: String) {
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
        try {
            startActivity(geoIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
