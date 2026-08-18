package com.example.sportsanalysis

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sportsanalysis.domain.pose.AndroidPoseDetector
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.pose.PoseDetector
import com.example.sportsanalysis.presentation.components.PoseSkeletonOverlay
import org.koin.compose.koinInject

@Composable
fun CameraPosePreview(
    latestFrame: PoseFrame?,
    highlightedLandmarkIndices: Set<Int>,
    modifier: Modifier
) {
    val detector: PoseDetector = koinInject()
    val androidDetector = detector as? AndroidPoseDetector ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lifecycleOwner) {
        androidDetector.previewProvider = { preview ->
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
        androidDetector.bindToLifecycle(lifecycleOwner)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        PoseSkeletonOverlay(
            frame = latestFrame,
            highlighted = highlightedLandmarkIndices,
            modifier = Modifier.fillMaxSize(),
            mirror = false
        )
    }
}
