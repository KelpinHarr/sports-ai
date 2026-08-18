package com.example.sportsanalysis.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportsanalysis.data.repository.AnalysisRepository
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureSignedIn()
                repository.observeHistory().collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            analyses = items,
                            errorMessage = null
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Failed to load history"
                    )
                }
            }
        }
    }
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val analyses: List<FreeThrowAnalysis> = emptyList(),
    val errorMessage: String? = null
)
