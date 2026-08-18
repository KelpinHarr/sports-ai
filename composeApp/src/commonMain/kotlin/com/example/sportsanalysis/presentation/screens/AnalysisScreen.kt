package com.example.sportsanalysis.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sportsanalysis.domain.framing.FramingStatus
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.example.sportsanalysis.presentation.components.CameraPosePreview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisScreen(
    onOpenHistory: () -> Unit,
    viewModel: AnalysisViewModel =
        koinViewModel()
) {
    val state by
    viewModel.uiState.collectAsState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    LaunchedEffect(
        state.errorMessage
    ) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message
            )

            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState
            )
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text =
                        "Free Throw Coach",
                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                TextButton(
                    onClick =
                        onOpenHistory
                ) {
                    Text("History")
                }
            }
        }
    ) { contentPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(Color.Black)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                CameraPosePreview(
                    latestFrame =
                        state.latestFrame,
                    highlightedLandmarkIndices =
                        state.highlightedLandmarks,
                    modifier =
                        Modifier.fillMaxSize()
                )

                StatusBadge(
                    state = state,
                    modifier = Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(top = 16.dp)
                )
            }

            ControlPanel(
                state = state,
                onToggleCapture =
                    viewModel::toggleCapture
            )
        }
    }
}

@Composable
private fun StatusBadge(
    state: AnalysisUiState,
    modifier: Modifier = Modifier
) {
    val text =
        when {
            state.isAnalyzing ->
                "Analyzing"

            state.isCapturing ->
                "Recording shot"

            !state.cameraReady ->
                "Starting camera"

            else ->
                state.framingResult.message
        }

    val color =
        when {
            state.isAnalyzing ->
                Color(0xFFFF9800)

            state.isCapturing ->
                Color(0xFFE53935)

            state.framingResult.status ==
                    FramingStatus.READY ->
                Color(0xFF2E7D32)

            else ->
                Color(0xFFFF9800)
        }

    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(50),
        color = color.copy(
            alpha = 0.9f
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 10.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )

                Spacer(
                    Modifier.size(8.dp)
                )
            }

            Text(
                text = text,
                color = Color.White,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ControlPanel(
    state: AnalysisUiState,
    onToggleCapture: () -> Unit
) {
    val canStart =
        state.cameraReady &&
                state.framingResult.isReady

    val buttonEnabled =
        !state.isAnalyzing &&
                (
                        state.isCapturing ||
                                canStart
                        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme
                    .colorScheme
                    .surface
            )
            .padding(16.dp)
    ) {

        if (
            !state.isCapturing &&
            state.cameraReady &&
            !state.framingResult.isReady
        ) {
            Text(
                text =
                    state.framingResult.message,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 8.dp
                    )
            )
        }

        Button(
            onClick =
                onToggleCapture,
            enabled =
                buttonEnabled,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                if (state.isCapturing) {
                    "Analyze Shot"
                } else {
                    "Start Shot"
                }
            )
        }

        state.result?.let { analysis ->
            Spacer(
                Modifier.height(12.dp)
            )

            AnalysisResultCard(
                analysis
            )
        }
    }
}

@Composable
private fun AnalysisResultCard(
    analysis: FreeThrowAnalysis
) {
    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            item {
                Text(
                    text =
                        "Overall ${analysis.overallScore}/100",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {
                Text(
                    "Accuracy ${analysis.accuracyScore}   " +
                            "Style ${analysis.styleScore}   " +
                            "Technique ${analysis.techniqueScore}"
                )
            }

            item {
                Text(
                    "Elbow ${analysis.metrics.elbowAngleDeg.toInt()}°   " +
                            "Knee ${analysis.metrics.kneeAngleDeg.toInt()}°   " +
                            "Wrist ${analysis.metrics.wristFollowThroughDeg.toInt()}°"
                )
            }

            analysis.aiFeedback
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let { feedback ->

                    item {
                        Text(
                            text =
                                "Gemini Coach",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight
                                    .SemiBold
                        )
                    }

                    item {
                        Text(feedback)
                    }
                }

            if (
                analysis.suggestions
                    .isNotEmpty()
            ) {
                item {
                    Text(
                        text =
                            "Suggestions",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight
                                .SemiBold
                    )
                }

                analysis.suggestions
                    .forEach { suggestion ->
                        item {
                            Text(
                                "• $suggestion"
                            )
                        }
                    }
            }
        }
    }
}