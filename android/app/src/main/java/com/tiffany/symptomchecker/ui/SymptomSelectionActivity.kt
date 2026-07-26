package com.tiffany.symptomchecker.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.tiffany.symptomchecker.R
import com.tiffany.symptomchecker.model.UiState
import com.tiffany.symptomchecker.viewmodel.SymptomViewModel
import kotlinx.coroutines.launch

class SymptomSelectionActivity : AppCompatActivity() {

    private val viewModel: SymptomViewModel by viewModels()
    private lateinit var checkboxContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnPredict: Button
    private lateinit var tvError: TextView
    private lateinit var spSeverity: Spinner
    private lateinit var spDuration: Spinner
    private var allPairs: List<Pair<String, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom_selection)
        supportActionBar?.title = "Select Symptoms"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        checkboxContainer = findViewById(R.id.checkboxContainer)
        progressBar = findViewById(R.id.progressBar)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        btnPredict = findViewById(R.id.btnPredict)
        tvError = findViewById(R.id.tvError)
        spSeverity = findViewById(R.id.spSeverity)
        spDuration = findViewById(R.id.spDuration)

        spSeverity.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Severity: Mild", "Severity: Moderate", "Severity: Severe")
        )
        spDuration.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Duration: Today", "Duration: 2–3 days", "Duration: About 1 week", "Duration: Longer than 1 week")
        )

        btnPredict.setOnClickListener {
            val selectedKeys = viewModel.selectedSymptoms.value.keys
            val redFlags = setOf("chest_pain", "breathlessness", "fainting", "coma", "blood_in_sputum", "acute_liver_failure")
            val hasRedFlag = selectedKeys.any { it in redFlags }
            val isSevere = spSeverity.selectedItemPosition == 2
            if (hasRedFlag && isSevere) {
                AlertDialog.Builder(this)
                    .setTitle("Urgent symptom warning")
                    .setMessage("One or more selected symptoms may require urgent medical attention. Continue only for informational guidance and seek emergency help if you feel seriously unwell.")
                    .setPositiveButton("Continue") { _, _ -> viewModel.predict() }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                viewModel.predict()
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            viewModel.clearSymptoms()
            renderSymptoms(allPairs)
        }

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.lowercase() ?: ""
                val filtered = if (q.isEmpty()) allPairs
                else allPairs.filter { it.second.lowercase().contains(q) }
                renderSymptoms(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        lifecycleScope.launch {
            viewModel.symptomsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        tvError.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        allPairs = state.data
                        renderSymptoms(allPairs)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        tvError.visibility = View.VISIBLE
                        tvError.text = state.message
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedSymptoms.collect { selected ->
                tvSelectedCount.text = "Selected: ${selected.size} / 15"
                btnPredict.isEnabled = selected.isNotEmpty()
                btnPredict.alpha = if (selected.isNotEmpty()) 1f else 0.5f
            }
        }

        lifecycleScope.launch {
            viewModel.predictionState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnPredict.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        btnPredict.isEnabled = true
                        val intent = Intent(this@SymptomSelectionActivity, ResultActivity::class.java)
                        intent.putExtra("RESULT_JSON", Gson().toJson(state.data))
                        intent.putExtra("SEVERITY", spSeverity.selectedItem.toString().substringAfter(": "))
                        intent.putExtra("DURATION", spDuration.selectedItem.toString().substringAfter(": "))
                        startActivity(intent)
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnPredict.isEnabled = true
                        tvError.visibility = View.VISIBLE
                        tvError.text = state.message
                    }
                    else -> {}
                }
            }
        }
    }

    private fun renderSymptoms(pairs: List<Pair<String, String>>) {
        checkboxContainer.removeAllViews()
        pairs.forEach { (key, display) ->
            val cb = CheckBox(this)
            cb.text = display
            cb.isChecked = viewModel.isSelected(key)
            cb.setPadding(16, 12, 16, 12)
            cb.textSize = 14f
            cb.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleSymptom(key, display)
                cb.post { cb.isChecked = viewModel.isSelected(key) }
            }
            checkboxContainer.addView(cb)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
