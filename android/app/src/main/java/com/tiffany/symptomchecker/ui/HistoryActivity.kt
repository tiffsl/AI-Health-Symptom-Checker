package com.tiffany.symptomchecker.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.tiffany.symptomchecker.R
import com.tiffany.symptomchecker.data.HistoryStore

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.title = "Check History"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        historyContainer = findViewById(R.id.historyContainer)
        emptyView = findViewById(R.id.tvEmptyHistory)

        findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear history?")
                .setMessage("This will remove all saved symptom checks from this device.")
                .setPositiveButton("Clear") { _, _ ->
                    HistoryStore.clear(this)
                    renderHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        renderHistory()
    }

    private fun renderHistory() {
        val entries = HistoryStore.getAll(this)
        historyContainer.removeAllViews()
        emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        entries.forEach { entry ->
            val card = CardView(this).apply {
                radius = 18f
                cardElevation = 4f
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 24, 28, 24)
            }
            fun line(text: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.parseColor(color))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 4, 0, 4)
            }

            content.addView(line(entry.checkedAt, 12f, "#777777"))
            content.addView(line(entry.topCondition, 18f, "#1F3864", true))
            content.addView(line("Confidence: ${entry.confidence.toInt()}%", 13f, "#333333"))
            content.addView(line("Urgency: ${entry.triageLevel}", 13f, "#2E75B6", true))
            content.addView(line("Severity: ${entry.severity} • Duration: ${entry.duration}", 12f, "#555555"))
            val symptoms = entry.symptoms.joinToString(", ") { it.replace("_", " ") }
            content.addView(line("Symptoms: $symptoms", 12f, "#555555"))
            card.addView(content)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
            historyContainer.addView(card, params)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
