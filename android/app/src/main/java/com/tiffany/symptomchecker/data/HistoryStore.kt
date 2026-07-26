package com.tiffany.symptomchecker.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tiffany.symptomchecker.model.PredictResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryEntry(
    val checkedAt: String,
    val topCondition: String,
    val confidence: Double,
    val triageLevel: String,
    val symptoms: List<String>,
    val severity: String = "Not specified",
    val duration: String = "Not specified"
)

object HistoryStore {
    private const val PREFS = "symptom_checker_history"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 20
    private val gson = Gson()

    fun save(context: Context, result: PredictResponse, severity: String, duration: String, triageOverride: String? = null) {
        val existing = getAll(context).toMutableList()
        val top = result.top3_predictions.firstOrNull()
        val entry = HistoryEntry(
            checkedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
            topCondition = top?.disease ?: "No prediction",
            confidence = top?.confidence ?: 0.0,
            triageLevel = triageOverride ?: result.triage_level,
            symptoms = result.symptoms_analysed,
            severity = severity,
            duration = duration
        )
        existing.add(0, entry)
        val trimmed = existing.take(MAX_ENTRIES)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, gson.toJson(trimmed))
            .apply()
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryEntry>>() {}.type
        return runCatching { gson.fromJson<List<HistoryEntry>>(json, type) }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
