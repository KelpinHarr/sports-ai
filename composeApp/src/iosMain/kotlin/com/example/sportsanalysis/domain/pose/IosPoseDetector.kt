package com.example.sportsanalysis.domain.pose

import com.example.sportsanalysis.domain.model.PoseFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow

internal class IosPoseDetector : PoseDetector {
    override fun initialize() = Unit
    override fun detectStream(): Flow<PoseFrame> = emptyFlow()
    override fun release() = Unit
}