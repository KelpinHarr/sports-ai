package com.example.sportsanalysis.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sportsanalysis.domain.pose.AndroidPoseDetector
import com.example.sportsanalysis.di.KoinHelper
import com.example.sportsanalysis.domain.model.PoseFrame
import java.util.concurrent.Executors

@Composable
actual fun CameraPosePreview(
    latestFrame: PoseFrame?,
    highlightedLandmarkIndices: Set<Int>,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detector = remember {
        KoinHelper.providePoseDetector() as AndroidPoseDetector
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(
        hasCameraPermission,
        lifecycleOwner,
        previewView
    ) {
        if (!hasCameraPermission) {
            return@LaunchedEffect
        }

        detector.initialize()

        detector.previewProvider = { preview ->
            preview.surfaceProvider = previewView.surfaceProvider
        }

        detector.bindToLifecycle(lifecycleOwner)
    }

    DisposableEffect(detector) {
        onDispose {
            detector.previewProvider = null
        }
    }

    Box(modifier = modifier) {
        if (hasCameraPermission) {
            AndroidView(
                factory = {
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            PoseSkeletonOverlay(
                frame = latestFrame,
                highlighted = highlightedLandmarkIndices,
                modifier = Modifier.fillMaxSize(),
                mirror = false
            )
        } else {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Allow camera")
            }
        }
    }
}