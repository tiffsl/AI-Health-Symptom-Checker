package com.tiffany.symptomchecker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiffany.symptomchecker.model.PredictRequest
import com.tiffany.symptomchecker.model.PredictResponse
import com.tiffany.symptomchecker.model.UiState
import com.tiffany.symptomchecker.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SymptomViewModel : ViewModel() {

    private val _symptomsState = MutableStateFlow<UiState<List<Pair<String, String>>>>(UiState.Idle)
    val symptomsState: StateFlow<UiState<List<Pair<String, String>>>> = _symptomsState

    private val _selectedSymptoms = MutableStateFlow<MutableMap<String, String>>(mutableMapOf())
    val selectedSymptoms: StateFlow<MutableMap<String, String>> = _selectedSymptoms

    private val _predictionState = MutableStateFlow<UiState<PredictResponse>>(UiState.Idle)
    val predictionState: StateFlow<UiState<PredictResponse>> = _predictionState

    init {
        fetchSymptoms()
    }

    fun fetchSymptoms() {
        viewModelScope.launch {
            _symptomsState.value = UiState.Loading
            try {
                val response = ApiClient.service.getSymptoms()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _symptomsState.value = UiState.Success(body.symptoms.zip(body.readable))
                } else {
                    _symptomsState.value = UiState.Error("Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _symptomsState.value = UiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun toggleSymptom(key: String, display: String) {
        val current = _selectedSymptoms.value.toMutableMap()
        if (current.containsKey(key)) {
            current.remove(key)
        } else {
            if (current.size >= 15) return
            current[key] = display
        }
        _selectedSymptoms.value = current
    }

    fun isSelected(key: String): Boolean = _selectedSymptoms.value.containsKey(key)

    fun clearSymptoms() {
        _selectedSymptoms.value = mutableMapOf()
        _predictionState.value = UiState.Idle
    }

    fun predict() {
        val symptoms = _selectedSymptoms.value.keys.toList()
        if (symptoms.isEmpty()) return
        viewModelScope.launch {
            _predictionState.value = UiState.Loading
            try {
                val response = ApiClient.service.predict(PredictRequest(symptoms))
                if (response.isSuccessful && response.body() != null) {
                    _predictionState.value = UiState.Success(response.body()!!)
                } else {
                    _predictionState.value = UiState.Error("Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _predictionState.value = UiState.Error("Network error: ${e.message}")
            }
        }
    }
}
