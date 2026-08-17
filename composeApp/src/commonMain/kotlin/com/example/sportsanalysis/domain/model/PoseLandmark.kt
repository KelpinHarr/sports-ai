package com.example.sportsanalysis.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PoseLandmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f
)

object PoseLandmarkIndex {
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
}

@Serializable
data class PoseFrame(
    val timestampMs: Long,
    val landmarks: List<PoseLandmark>
)
