package com.example.sportsanalysis.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportsanalysis.data.repository.AnalysisRepository
import com.example.sportsanalysis.domain.analysis.FreeThrowAnalyzer
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.pose.PoseDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AnalysisViewModel(
    private val poseDetector: PoseDetector,
    private val analyzer: FreeThrowAnalyzer,
    private val repo: AnalysisRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalysisUiState())
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val frameBuffer = mutableListOf<PoseFrame>()
    private var collectorJob: Job? = null

    fun onScreenStarted() {
        viewModelScope.launch {
            repo.ensureSignedIn()
            poseDetector.initialize()
            _state.update { it.copy(detectorReady = true) }
        }
    }

    fun startRecording() {
        if (_state.value.phase != Phase.Idle && _state.value.phase != Phase.ResultReady) return
        frameBuffer.clear()
        _state.update { it.copy(phase = Phase.Recording, latestResult = null) }

        collectorJob = viewModelScope.launch {
            poseDetector.detectStream().collect { frame ->
                frameBuffer.add(frame)
                _state.update { it.copy(latestFrame = frame) }
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        if (_state.value.phase != Phase.Recording) return
        collectorJob?.cancel()
        _state.update { it.copy(phase = Phase.Analyzing) }

        viewModelScope.launch {
            val result = analyzer.analyze(frameBuffer.toList())
            if (result == null) {
                _state.update {
                    it.copy(
                        phase = Phase.Idle,
                        error = "Couldn't detect a clear shot motion. Try again with the full body in frame."
                    )
                }
                return@launch
            }
            runCatching { repo.save(result.copy(timestampMs = Clock.System.now().toEpochMilliseconds())) }
            _state.update { it.copy(phase = Phase.ResultReady, latestResult = result) }
        }
    }

    fun dismissResult() {
        _state.update { it.copy(phase = Phase.Idle, latestResult = null) }
    }

    override fun onCleared() {
        viewModelScope.launch { poseDetector.release() }
        super.onCleared()
    }
}

data class AnalysisUiState(
    val detectorReady: Boolean = false,
    val phase: Phase = Phase.Idle,
    val latestFrame: PoseFrame? = null,
    val latestResult: FreeThrowAnalysis? = null,
    val error: String? = null
)

enum class Phase { Idle, Recording, Analyzing, ResultReady }