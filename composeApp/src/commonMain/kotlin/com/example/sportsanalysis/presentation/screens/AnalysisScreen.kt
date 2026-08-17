package com.example.sportsanalysis.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sportsanalysis.domain.model.BodyArea
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.example.sportsanalysis.domain.model.PoseLandmarkIndex as L
import com.example.sportsanalysis.presentation.components.CameraPosePreview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onOpenHistory: () -> Unit,
    viewModel: AnalysisViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.onScreenStarted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free Throw Coach") },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("History") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            CameraPosePreview(
                latestFrame = state.latestFrame,
                highlightedLandmarkIndices = state.latestResult?.toHighlightSet() ?: emptySet(),
                modifier = Modifier.fillMaxSize()
            )

            StatusPill(
                phase = state.phase,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            )

            RecordButton(
                phase = state.phase,
                onStart = viewModel::startRecording,
                onStop = viewModel::stopRecordingAndAnalyze,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
            )

            AnimatedVisibility(
                visible = state.phase == Phase.ResultReady && state.latestResult != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                state.latestResult?.let { ResultSheet(it, onDismiss = viewModel::dismissResult) }
            }

            state.error?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    dismissAction = {
                        TextButton(
                            onClick = {
                                viewModel.dismissError()
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(phase: Phase, modifier: Modifier = Modifier) {
    val (label, color) = when (phase) {
        Phase.Idle -> "Ready" to Color(0xFF1565C0)
        Phase.Recording -> "● Recording" to Color(0xFFE53935)
        Phase.Analyzing -> "Analyzing…" to Color(0xFFFF8F00)
        Phase.ResultReady -> "Result" to Color(0xFF2E7D32)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color
    ) {
        Text(
            label, color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RecordButton(
    phase: Phase,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = phase == Phase.Recording
    FloatingActionButton(
        onClick = if (isRecording) onStop else onStart,
        containerColor = if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
        modifier = modifier.size(72.dp),
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isRecording) "Stop" else "Record",
            tint = Color.White
        )
    }
}

@Composable
private fun ResultSheet(analysis: FreeThrowAnalysis, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Text("Shot Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreChip("Accuracy", analysis.accuracyScore, Modifier.weight(1f))
                ScoreChip("Style", analysis.styleScore, Modifier.weight(1f))
                ScoreChip("Technique", analysis.techniqueScore, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            ScoreChip("Overall", analysis.overallScore, Modifier.fillMaxWidth(), big = true)

            Spacer(Modifier.height(16.dp))
            Text("What we saw", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            MetricsRow(analysis)

            Spacer(Modifier.height(16.dp))
            Text("Suggestions", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                analysis.suggestions.forEach { tip ->
                    Row {
                        Text("•  ", fontWeight = FontWeight.Bold)
                        Text(tip)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Try another shot")
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: Int, modifier: Modifier, big: Boolean = false) {
    val color = when {
        value >= 80 -> Color(0xFF2E7D32)
        value >= 60 -> Color(0xFFFF8F00)
        else -> Color(0xFFC62828)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(if (big) 16.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = if (big) 14.sp else 12.sp)
            Text(
                "$value", fontSize = if (big) 32.sp else 22.sp,
                fontWeight = FontWeight.Bold, color = color
            )
        }
    }
}

@Composable
private fun MetricsRow(a: FreeThrowAnalysis) {
    val m = a.metrics
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MetricLine("Elbow angle at release", "${m.elbowAngleDeg.toInt()}°", "ideal 85–95°")
        MetricLine("Knee bend at dip", "${m.kneeAngleDeg.toInt()}°", "ideal 115–135°")
        MetricLine(
            "Wrist follow-through", "${m.wristFollowThroughDeg.toInt()}°",
            "lower = better snap"
        )
    }
}

@Composable
private fun MetricLine(name: String, value: String, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
        Text(
            "  ($hint)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

private fun FreeThrowAnalysis.toHighlightSet(): Set<Int> = buildSet {
    issues.forEach { issue ->
        when (issue.area) {
            BodyArea.SHOOTING_HAND, BodyArea.ELBOW -> {
                add(L.RIGHT_ELBOW); add(L.RIGHT_WRIST)
            }
            BodyArea.SHOULDER -> { add(L.RIGHT_SHOULDER); add(L.LEFT_SHOULDER) }
            BodyArea.KNEES -> { add(L.LEFT_KNEE); add(L.RIGHT_KNEE) }
            BodyArea.FEET -> { add(L.LEFT_ANKLE); add(L.RIGHT_ANKLE) }
            BodyArea.HIPS -> { add(L.LEFT_HIP); add(L.RIGHT_HIP) }
            BodyArea.HEAD -> add(L.NOSE)
            BodyArea.GUIDE_HAND -> { add(L.LEFT_WRIST); add(L.LEFT_ELBOW) }
        }
    }
}
