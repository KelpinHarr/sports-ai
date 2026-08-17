package com.example.sportsanalysis.domain.pose

import com.example.sportsanalysis.domain.model.PoseFrame
import kotlinx.coroutines.flow.Flow

interface PoseDetector {
    suspend fun initialize()
    fun detectStream(): Flow<PoseFrame>
    suspend fun release()
}

expect class PoseDetectorFactory {
    fun create(): PoseDetector
}

