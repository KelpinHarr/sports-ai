package com.example.sportsanalysis.domain.pose

import com.example.sportsanalysis.domain.model.PoseFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class IosPoseDetector : PoseDetector {
    private val frames = MutableSharedFlow<PoseFrame>()
    override suspend fun initialize() { /* no-op stub */ }
    override fun detectStream(): Flow<PoseFrame> = frames.asSharedFlow()
    override suspend fun release() { /* no-op stub */ }
}

actual class PoseDetectorFactory {
    actual fun create(): PoseDetector = IosPoseDetector()
}