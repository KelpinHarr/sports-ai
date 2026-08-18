package com.example.sportsanalysis.domain.model
import kotlinx.serialization.Serializable

@Serializable
data class FreeThrowAnalysis(
    val id: String = "",
    val userId: String = "",
    val timestampMs: Long,
    val accuracyScore: Int,
    val styleScore: Int,
    val techniqueScore: Int,
    val overallScore: Int,
    val metrics: BiomechanicalMetrics,
    val issues: List<FormIssue>,
    val suggestions: List<String>,
    val aiFeedback: String? = null,
    val videoUrl: String? = null
)

@Serializable
data class BiomechanicalMetrics(
    val elbowAngleDeg: Float,
    val kneeAngleDeg: Float,
    val wristFollowThroughDeg: Float,
    val elbowAlignmentOffset: Float,
    val releaseHeightRatio: Float,
    val stanceBalance: Float
)

@Serializable
data class FormIssue(
    val area: BodyArea,
    val severity: Severity,
    val message: String
)

@Serializable
enum class BodyArea {
    SHOOTING_HAND,
    GUIDE_HAND,
    ELBOW,
    SHOULDER,
    KNEES,
    FEET,
    HIPS,
    HEAD
}

@Serializable
enum class Severity {
    MINOR,
    MODERATE,
    MAJOR
}
