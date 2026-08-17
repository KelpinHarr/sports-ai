package com.example.sportsanalysis.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportsanalysis.data.repository.AnalysisRepository
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repo: AnalysisRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState(isLoading = true))
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureSignedIn()
            repo.observeHistory()
                .catch { e -> _state.value = HistoryUiState(error = e.message) }
                .collectLatest { list ->
                    _state.value = HistoryUiState(items = list, isLoading = false)
                }
        }
    }
}

data class HistoryUiState(
    val items: List<FreeThrowAnalysis> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)