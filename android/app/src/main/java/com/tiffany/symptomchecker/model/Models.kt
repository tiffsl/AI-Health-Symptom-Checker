package com.tiffany.symptomchecker.model

data class PredictRequest(val symptoms: List<String>)

data class PredictResponse(
    val top3_predictions: List<Prediction>,
    val triage_level: String,
    val triage_advice: TriageAdvice,
    val red_flags_triggered: List<String>,
    val symptoms_analysed: List<String>,
    val disclaimer: String
)

data class Prediction(
    val disease: String,
    val confidence: Double,
    val triage: String
)

data class TriageAdvice(
    val message: String,
    val color: String,
    val icon: String
)

data class SymptomsResponse(
    val symptoms: List<String>,
    val readable: List<String>,
    val count: Int
)

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
