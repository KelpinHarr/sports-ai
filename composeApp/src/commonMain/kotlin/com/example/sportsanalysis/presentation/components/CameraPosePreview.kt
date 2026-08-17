package com.example.sportsanalysis.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sportsanalysis.domain.model.PoseFrame

@Composable
expect fun CameraPosePreview(
    latestFrame: PoseFrame?,
    highlightedLandmarkIndices: Set<Int>,
    modifier: Modifier
)