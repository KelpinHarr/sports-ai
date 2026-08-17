package com.example.sportsanalysis.domain.pose

import android.content.Context
import com.example.sportsanalysis.domain.model.PoseFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidPoseDetector(
    private val context: Context
) : PoseDetector {
    private val frames = MutableSharedFlow<PoseFrame>()

    override suspend fun initialize() {
        // TODO: Initialize MediaPipe pose detector here
    }

    override fun detectStream(): Flow<PoseFrame> = frames.asSharedFlow()

    override suspend fun release() {
        // TODO: Release MediaPipe resources
    }
}

actual class PoseDetectorFactory(
    private val context: Context
) {
    actual fun create(): PoseDetector = AndroidPoseDetector(context)
}