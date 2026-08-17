package com.example.sportsanalysis.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.sportsanalysis.domain.model.PoseFrame

@Composable
actual fun CameraPosePreview(
    latestFrame: PoseFrame?,
    highlightedLandmarkIndices: Set<Int>,
    modifier: Modifier
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Text("Camera preview not implemented on iOS yet", color = Color.White)
        PoseSkeletonOverlay(
            frame = latestFrame,
            highlighted = highlightedLandmarkIndices,
            modifier = Modifier.fillMaxSize(),
            mirror = false
        )
    }
}