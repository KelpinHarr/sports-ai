package com.example.sportsanalysis.domain.analysis

import com.example.sportsanalysis.domain.model.BiomechanicalMetrics
import com.example.sportsanalysis.domain.model.BodyArea
import com.example.sportsanalysis.domain.model.FormIssue
import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmark
import com.example.sportsanalysis.domain.model.PoseLandmarkIndex
import com.example.sportsanalysis.domain.model.Severity
import kotlin.math.abs
import kotlin.math.absoluteValue

class FreeThrowAnalyzer(
    private val handedness: Handedness = Handedness.RIGHT
) {

    enum class Handedness { LEFT, RIGHT }

    fun analyze(frames: List<PoseFrame>): FreeThrowAnalysis? {
        if (frames.size < 10) return null

        val releaseFrame = findReleaseFrame(frames) ?: return null
        val dipFrame = findDipFrame(frames) ?: releaseFrame

        val metrics = computeMetrics(releaseFrame, dipFrame)
        val issues = detectIssues(metrics)
        val (acc, style, tech) = scoreFromMetrics(metrics, issues)
        val overall = ((acc + style + tech) / 3.0).toInt()

        return FreeThrowAnalysis(
            timestampMs = frames.last().timestampMs,
            accuracyScore = acc,
            styleScore = style,
            techniqueScore = tech,
            overallScore = overall,
            metrics = metrics,
            issues = issues,
            suggestions = buildSuggestions(issues, metrics)
        )
    }

    private fun findReleaseFrame(frames: List<PoseFrame>): PoseFrame? {
        val wristIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.RIGHT_WRIST else PoseLandmarkIndex.LEFT_WRIST
        return frames.minByOrNull { f ->
            f.landmarks.getOrNull(wristIdx)?.y ?: Float.MAX_VALUE
        }
    }

    private fun findDipFrame(frames: List<PoseFrame>): PoseFrame? {
        return frames.maxByOrNull { f ->
            val lh = f.landmarks.getOrNull(PoseLandmarkIndex.LEFT_HIP)?.y ?: 0f
            val rh = f.landmarks.getOrNull(PoseLandmarkIndex.RIGHT_HIP)?.y ?: 0f
            (lh + rh) / 2f
        }
    }


    private fun computeMetrics(release: PoseFrame, dip: PoseFrame): BiomechanicalMetrics {
        val rL = release.landmarks
        val dL = dip.landmarks

        val (shoulder, elbow, wrist, hip, knee, ankle, oppositeAnkle) = if (handedness == Handedness.RIGHT) {
            JointSet(
                rL[PoseLandmarkIndex.RIGHT_SHOULDER],
                rL[PoseLandmarkIndex.RIGHT_ELBOW],
                rL[PoseLandmarkIndex.RIGHT_WRIST],
                dL[PoseLandmarkIndex.RIGHT_HIP],
                dL[PoseLandmarkIndex.RIGHT_KNEE],
                dL[PoseLandmarkIndex.RIGHT_ANKLE],
                dL[PoseLandmarkIndex.LEFT_ANKLE]
            )
        } else {
            JointSet(
                rL[PoseLandmarkIndex.LEFT_SHOULDER],
                rL[PoseLandmarkIndex.LEFT_ELBOW],
                rL[PoseLandmarkIndex.LEFT_WRIST],
                dL[PoseLandmarkIndex.LEFT_HIP],
                dL[PoseLandmarkIndex.LEFT_KNEE],
                dL[PoseLandmarkIndex.LEFT_ANKLE],
                dL[PoseLandmarkIndex.RIGHT_ANKLE]
            )
        }

        val elbowAngle = Geometry.angleDeg(shoulder, elbow, wrist)

        val kneeAngle = Geometry.angleDeg(hip, knee, ankle)

        val indexIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.RIGHT_INDEX else PoseLandmarkIndex.LEFT_INDEX
        val index = rL.getOrNull(indexIdx) ?: wrist
        val wristAngle = Geometry.angleDeg(elbow, wrist, index)

        val elbowAlign = Geometry.horizontalOffset(elbow, shoulder)

        val nose = rL[PoseLandmarkIndex.NOSE]
        val bodyHeight = (nose.y - ankle.y).absoluteValue.coerceAtLeast(0.001f)
        val releaseAboveShoulder = (shoulder.y - wrist.y).coerceAtLeast(0f)
        val releaseRatio = (releaseAboveShoulder / bodyHeight).coerceIn(0f, 1f)

        val stanceBalance = 1f - Geometry.horizontalOffset(ankle, oppositeAnkle).coerceIn(0f, 1f)

        return BiomechanicalMetrics(
            elbowAngleDeg = elbowAngle,
            kneeAngleDeg = kneeAngle,
            wristFollowThroughDeg = wristAngle,
            elbowAlignmentOffset = elbowAlign,
            releaseHeightRatio = releaseRatio,
            stanceBalance = stanceBalance
        )
    }

    private data class JointSet(
        val shoulder: PoseLandmark, val elbow: PoseLandmark, val wrist: PoseLandmark,
        val hip: PoseLandmark, val knee: PoseLandmark, val ankle: PoseLandmark,
        val oppositeAnkle: PoseLandmark
    )

    private fun detectIssues(m: BiomechanicalMetrics): List<FormIssue> = buildList {
        when {
            m.elbowAngleDeg < 70 -> add(FormIssue(
                BodyArea.ELBOW, Severity.MAJOR,
                "Shooting elbow is too bent (${m.elbowAngleDeg.toInt()}°). Aim for ~90° at release."
            ))
            m.elbowAngleDeg < 80 -> add(FormIssue(
                BodyArea.ELBOW, Severity.MINOR,
                "Elbow slightly too bent — try to fully extend on release."
            ))
            m.elbowAngleDeg > 170 -> Unit
            m.elbowAngleDeg in 95f..170f -> Unit
        }

        if (m.elbowAlignmentOffset > 0.08f) add(FormIssue(
            BodyArea.SHOOTING_HAND, Severity.MODERATE,
            "Shooting elbow is flared out — keep it tucked under the ball."
        ))

        when {
            m.kneeAngleDeg > 160 -> add(FormIssue(
                BodyArea.KNEES, Severity.MAJOR,
                "Legs barely bent (${m.kneeAngleDeg.toInt()}°). Bend your knees to generate power from the ground."
            ))
            m.kneeAngleDeg > 145 -> add(FormIssue(
                BodyArea.KNEES, Severity.MINOR,
                "Slightly stiff legs — a deeper dip will give you a smoother shot."
            ))
            m.kneeAngleDeg < 90 -> add(FormIssue(
                BodyArea.KNEES, Severity.MODERATE,
                "Squatting too deep — wastes energy and slows release."
            ))
        }

        if (m.wristFollowThroughDeg > 150) add(FormIssue(
            BodyArea.SHOOTING_HAND, Severity.MODERATE,
            "Limp follow-through — snap the wrist down for backspin."
        ))

        if (m.releaseHeightRatio < 0.10f) add(FormIssue(
            BodyArea.SHOULDER, Severity.MINOR,
            "Release point is low — finish high, with arm fully extended."
        ))

        if (m.stanceBalance < 0.85f) add(FormIssue(
            BodyArea.FEET, Severity.MINOR,
            "Feet are uneven — set a balanced, shoulder-width stance."
        ))
    }


    private fun scoreFromMetrics(m: BiomechanicalMetrics, issues: List<FormIssue>): Triple<Int, Int, Int> {
        val techPenalty = issues.sumOf {
            when (it.severity) {
                Severity.MINOR -> 5
                Severity.MODERATE -> 12
                Severity.MAJOR -> 22
            }
        }
        val technique = (100 - techPenalty).coerceIn(0, 100)

        val elbowDelta = abs(m.elbowAngleDeg - 90f).coerceAtMost(45f)
        val kneeDelta = abs(m.kneeAngleDeg - 125f).coerceAtMost(45f)
        val style = (100 - (elbowDelta + kneeDelta).toInt()).coerceIn(0, 100)

        val alignBonus = (1f - (m.elbowAlignmentOffset * 8f).coerceIn(0f, 1f)) * 35f
        val heightBonus = (m.releaseHeightRatio * 4f).coerceIn(0f, 1f) * 30f
        val followBonus = ((180f - m.wristFollowThroughDeg).coerceIn(0f, 110f) / 110f) * 35f
        val accuracy = (alignBonus + heightBonus + followBonus).toInt().coerceIn(0, 100)

        return Triple(accuracy, style, technique)
    }


    private fun buildSuggestions(issues: List<FormIssue>, m: BiomechanicalMetrics): List<String> {
        val tips = issues.map { it.message }.toMutableList()
        if (tips.isEmpty()) {
            tips += "Great form! Keep your routine consistent and trust the process."
        } else {
            tips += "Try the BEEF cue: Balance, Eyes on target, Elbow under, Follow-through."
        }
        return tips
    }
}