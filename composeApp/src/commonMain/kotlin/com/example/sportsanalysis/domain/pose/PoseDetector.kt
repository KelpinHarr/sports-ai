package com.example.sportsanalysis.domain.pose

import com.example.sportsanalysis.domain.model.PoseFrame
import kotlinx.coroutines.flow.Flow

interface PoseDetector {
    fun initialize()
    fun detectStream(): Flow<PoseFrame>
    fun release()
}