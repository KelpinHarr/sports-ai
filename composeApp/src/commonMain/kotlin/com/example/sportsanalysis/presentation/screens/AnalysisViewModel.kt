package com.example.sportsanalysis.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportsanalysis.data.repository.AnalysisRepository
import com.example.sportsanalysis.domain.ai.AiCoach
import com.example.sportsanalysis.domain.analysis.FreeThrowAnalyzer
import com.example.sportsanalysis.domain.framing.FramingResult
import com.example.sportsanalysis.domain.framing.FramingStatus
import com.example.sportsanalysis.domain.framing.evaluateFraming
import com.example.sportsanalysis.domain.model.BodyArea
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmarkIndex
import com.example.sportsanalysis.domain.pose.PoseDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val repository: AnalysisRepository,
    private val analyzer: FreeThrowAnalyzer,
    private val poseDetector: PoseDetector,
    private val aiCoach: AiCoach
) : ViewModel() {

    private val capturedFrames =
        mutableListOf<PoseFrame>()

    private val _uiState =
        MutableStateFlow(AnalysisUiState())

    val uiState: StateFlow<AnalysisUiState> =
        _uiState.asStateFlow()

    init {
        poseDetector.initialize()

        viewModelScope.launch {
            runCatching {
                repository.ensureSignedIn()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        errorMessage =
                            throwable.message
                                ?: "Firebase sign-in failed"
                    )
                }
            }
        }

        viewModelScope.launch {
            poseDetector
                .detectStream()
                .collect { frame ->

                    val framingResult =
                        evaluateFraming(frame)

                    _uiState.update {
                        it.copy(
                            latestFrame = frame,
                            cameraReady = true,
                            framingResult = framingResult
                        )
                    }

                    if (_uiState.value.isCapturing) {
                        if (
                            capturedFrames.size >=
                            MAX_CAPTURED_FRAMES
                        ) {
                            capturedFrames.removeAt(0)
                        }

                        capturedFrames += frame
                    }
                }
        }
    }

    fun toggleCapture() {
        if (_uiState.value.isCapturing) {
            finishCapture()
        } else {
            startCapture()
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun startCapture() {
        val state = _uiState.value

        if (!state.framingResult.isReady) {
            _uiState.update {
                it.copy(
                    errorMessage =
                        state.framingResult.message
                )
            }
            return
        }

        capturedFrames.clear()

        _uiState.update {
            it.copy(
                isCapturing = true,
                isAnalyzing = false,
                result = null,
                highlightedLandmarks = emptySet(),
                errorMessage = null
            )
        }
    }

    private fun finishCapture() {
        _uiState.update {
            it.copy(
                isCapturing = false,
                isAnalyzing = true
            )
        }

        val frames =
            capturedFrames.toList()

        viewModelScope.launch {
            val localAnalysis =
                analyzer.analyze(frames)

            if (localAnalysis == null) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage =
                            "Couldn't detect a clear shot motion. Try again with the full body in frame."
                    )
                }

                return@launch
            }

            val aiFeedback =
                runCatching {
                    aiCoach.createFeedback(
                        localAnalysis
                    )
                }.getOrNull()

            val completedAnalysis =
                localAnalysis.copy(
                    aiFeedback = aiFeedback
                )

            runCatching {
                repository.save(
                    completedAnalysis
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        errorMessage =
                            throwable.message
                                ?: "Analysis was completed but could not be saved"
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    result = completedAnalysis,
                    highlightedLandmarks =
                        highlightedLandmarksFor(
                            completedAnalysis
                        )
                )
            }
        }
    }

    private fun highlightedLandmarksFor(
        analysis: FreeThrowAnalysis
    ): Set<Int> = buildSet {

        analysis.issues.forEach { issue ->

            when (issue.area) {

                BodyArea.SHOOTING_HAND -> {
                    addAll(
                        listOf(
                            PoseLandmarkIndex.RIGHT_ELBOW,
                            PoseLandmarkIndex.RIGHT_WRIST,
                            PoseLandmarkIndex.RIGHT_INDEX
                        )
                    )
                }

                BodyArea.ELBOW -> {
                    add(
                        PoseLandmarkIndex.RIGHT_ELBOW
                    )
                }

                BodyArea.SHOULDER -> {
                    add(
                        PoseLandmarkIndex.RIGHT_SHOULDER
                    )
                }

                BodyArea.KNEES -> {
                    addAll(
                        listOf(
                            PoseLandmarkIndex.LEFT_KNEE,
                            PoseLandmarkIndex.RIGHT_KNEE
                        )
                    )
                }

                BodyArea.FEET -> {
                    addAll(
                        listOf(
                            PoseLandmarkIndex.LEFT_ANKLE,
                            PoseLandmarkIndex.RIGHT_ANKLE
                        )
                    )
                }

                BodyArea.HIPS -> {
                    addAll(
                        listOf(
                            PoseLandmarkIndex.LEFT_HIP,
                            PoseLandmarkIndex.RIGHT_HIP
                        )
                    )
                }

                BodyArea.HEAD -> {
                    add(
                        PoseLandmarkIndex.NOSE
                    )
                }

                BodyArea.GUIDE_HAND -> {
                    addAll(
                        listOf(
                            PoseLandmarkIndex.LEFT_ELBOW,
                            PoseLandmarkIndex.LEFT_WRIST
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        poseDetector.release()
        super.onCleared()
    }

    private companion object {
        const val MAX_CAPTURED_FRAMES = 240
    }
}

data class AnalysisUiState(
    val cameraReady: Boolean = false,

    val latestFrame: PoseFrame? = null,

    val framingResult: FramingResult =
        FramingResult(
            status =
                FramingStatus.BODY_NOT_VISIBLE,
            message =
                "Stand in front of the camera"
        ),

    val isCapturing: Boolean = false,

    val isAnalyzing: Boolean = false,

    val result: FreeThrowAnalysis? = null,

    val highlightedLandmarks: Set<Int> =
        emptySet(),

    val errorMessage: String? = null
)