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
    enum class Handedness {
        LEFT,
        RIGHT
    }

    fun analyze(frames: List<PoseFrame>): FreeThrowAnalysis? {
        if (frames.size < 10) return null

        val usableFrames = frames.filter(::hasRequiredLandmarks)
        if (usableFrames.size < 10) return null

        val releaseFrame = findReleaseFrame(usableFrames) ?: return null
        val dipFrame = findDipFrame(usableFrames) ?: return null

        val metrics = computeMetrics(releaseFrame, dipFrame)
        val issues = detectIssues(metrics)
        val (accuracy, style, technique) = scoreFromMetrics(metrics, issues)
        val overall = ((accuracy + style + technique) / 3.0).toInt()

        return FreeThrowAnalysis(
            timestampMs = usableFrames.last().timestampMs,
            accuracyScore = accuracy,
            styleScore = style,
            techniqueScore = technique,
            overallScore = overall,
            metrics = metrics,
            issues = issues,
            suggestions = buildSuggestions(issues)
        )
    }

    private fun hasRequiredLandmarks(frame: PoseFrame): Boolean {
        val shootingSide = if (handedness == Handedness.RIGHT) {
            listOf(
                PoseLandmarkIndex.RIGHT_SHOULDER,
                PoseLandmarkIndex.RIGHT_ELBOW,
                PoseLandmarkIndex.RIGHT_WRIST,
                PoseLandmarkIndex.RIGHT_HIP,
                PoseLandmarkIndex.RIGHT_KNEE,
                PoseLandmarkIndex.RIGHT_ANKLE,
                PoseLandmarkIndex.LEFT_ANKLE
            )
        } else {
            listOf(
                PoseLandmarkIndex.LEFT_SHOULDER,
                PoseLandmarkIndex.LEFT_ELBOW,
                PoseLandmarkIndex.LEFT_WRIST,
                PoseLandmarkIndex.LEFT_HIP,
                PoseLandmarkIndex.LEFT_KNEE,
                PoseLandmarkIndex.LEFT_ANKLE,
                PoseLandmarkIndex.RIGHT_ANKLE
            )
        }

        val required = shootingSide + listOf(
            PoseLandmarkIndex.NOSE,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.RIGHT_HIP
        )

        return required.distinct().all { index ->
            frame.landmarks.getOrNull(index)?.visibility?.let { it >= 0.5f } == true
        }
    }

    private fun findReleaseFrame(frames: List<PoseFrame>): PoseFrame? {
        val wristIndex = if (handedness == Handedness.RIGHT) {
            PoseLandmarkIndex.RIGHT_WRIST
        } else {
            PoseLandmarkIndex.LEFT_WRIST
        }

        return frames.minByOrNull { frame ->
            frame.landmarks[wristIndex].y
        }
    }

    private fun findDipFrame(frames: List<PoseFrame>): PoseFrame? {
        return frames.maxByOrNull { frame ->
            val leftHip = frame.landmarks[PoseLandmarkIndex.LEFT_HIP].y
            val rightHip = frame.landmarks[PoseLandmarkIndex.RIGHT_HIP].y
            (leftHip + rightHip) / 2f
        }
    }

    private fun computeMetrics(
        release: PoseFrame,
        dip: PoseFrame
    ): BiomechanicalMetrics {
        val releaseLandmarks = release.landmarks
        val dipLandmarks = dip.landmarks

        val joints = if (handedness == Handedness.RIGHT) {
            JointSet(
                shoulder = releaseLandmarks[PoseLandmarkIndex.RIGHT_SHOULDER],
                elbow = releaseLandmarks[PoseLandmarkIndex.RIGHT_ELBOW],
                wrist = releaseLandmarks[PoseLandmarkIndex.RIGHT_WRIST],
                hip = dipLandmarks[PoseLandmarkIndex.RIGHT_HIP],
                knee = dipLandmarks[PoseLandmarkIndex.RIGHT_KNEE],
                ankle = dipLandmarks[PoseLandmarkIndex.RIGHT_ANKLE],
                oppositeAnkle = dipLandmarks[PoseLandmarkIndex.LEFT_ANKLE]
            )
        } else {
            JointSet(
                shoulder = releaseLandmarks[PoseLandmarkIndex.LEFT_SHOULDER],
                elbow = releaseLandmarks[PoseLandmarkIndex.LEFT_ELBOW],
                wrist = releaseLandmarks[PoseLandmarkIndex.LEFT_WRIST],
                hip = dipLandmarks[PoseLandmarkIndex.LEFT_HIP],
                knee = dipLandmarks[PoseLandmarkIndex.LEFT_KNEE],
                ankle = dipLandmarks[PoseLandmarkIndex.LEFT_ANKLE],
                oppositeAnkle = dipLandmarks[PoseLandmarkIndex.RIGHT_ANKLE]
            )
        }

        val indexIndex = if (handedness == Handedness.RIGHT) {
            PoseLandmarkIndex.RIGHT_INDEX
        } else {
            PoseLandmarkIndex.LEFT_INDEX
        }

        val index = releaseLandmarks.getOrNull(indexIndex)
            ?.takeIf { it.visibility >= 0.5f }
            ?: joints.wrist

        val elbowAngle = Geometry.angleDeg(joints.shoulder, joints.elbow, joints.wrist)
        val kneeAngle = Geometry.angleDeg(joints.hip, joints.knee, joints.ankle)
        val wristAngle = Geometry.angleDeg(joints.elbow, joints.wrist, index)
        val elbowAlignment = Geometry.horizontalOffset(joints.elbow, joints.shoulder)

        val nose = releaseLandmarks[PoseLandmarkIndex.NOSE]
        val bodyHeight = (nose.y - joints.ankle.y).absoluteValue.coerceAtLeast(0.001f)
        val releaseAboveShoulder = (joints.shoulder.y - joints.wrist.y).coerceAtLeast(0f)
        val releaseRatio = (releaseAboveShoulder / bodyHeight).coerceIn(0f, 1f)
        val stanceBalance = 1f - Geometry.horizontalOffset(
            joints.ankle,
            joints.oppositeAnkle
        ).coerceIn(0f, 1f)

        return BiomechanicalMetrics(
            elbowAngleDeg = elbowAngle,
            kneeAngleDeg = kneeAngle,
            wristFollowThroughDeg = wristAngle,
            elbowAlignmentOffset = elbowAlignment,
            releaseHeightRatio = releaseRatio,
            stanceBalance = stanceBalance
        )
    }

    private fun detectIssues(metrics: BiomechanicalMetrics): List<FormIssue> = buildList {
        when {
            metrics.elbowAngleDeg < 70f -> add(
                FormIssue(
                    BodyArea.ELBOW,
                    Severity.MAJOR,
                    "Shooting elbow is too bent (${metrics.elbowAngleDeg.toInt()}°)."
                )
            )

            metrics.elbowAngleDeg < 80f -> add(
                FormIssue(
                    BodyArea.ELBOW,
                    Severity.MINOR,
                    "Elbow is slightly too bent at release."
                )
            )
        }

        if (metrics.elbowAlignmentOffset > 0.08f) {
            add(
                FormIssue(
                    BodyArea.SHOOTING_HAND,
                    Severity.MODERATE,
                    "Shooting elbow is flared out. Keep it more directly under the ball."
                )
            )
        }

        when {
            metrics.kneeAngleDeg > 160f -> add(
                FormIssue(
                    BodyArea.KNEES,
                    Severity.MAJOR,
                    "Leg drive is limited. Bend your knees more before extending."
                )
            )

            metrics.kneeAngleDeg > 145f -> add(
                FormIssue(
                    BodyArea.KNEES,
                    Severity.MINOR,
                    "A slightly deeper knee dip can improve power transfer."
                )
            )

            metrics.kneeAngleDeg < 90f -> add(
                FormIssue(
                    BodyArea.KNEES,
                    Severity.MODERATE,
                    "The knee dip is very deep and may slow the release."
                )
            )
        }

        if (metrics.wristFollowThroughDeg > 150f) {
            add(
                FormIssue(
                    BodyArea.SHOOTING_HAND,
                    Severity.MODERATE,
                    "Wrist follow-through is limited. Finish with a stronger wrist snap."
                )
            )
        }

        if (metrics.releaseHeightRatio < 0.10f) {
            add(
                FormIssue(
                    BodyArea.SHOULDER,
                    Severity.MINOR,
                    "Release point is low relative to body height."
                )
            )
        }

        if (metrics.stanceBalance < 0.85f) {
            add(
                FormIssue(
                    BodyArea.FEET,
                    Severity.MINOR,
                    "Stance appears uneven. Keep a stable, repeatable base."
                )
            )
        }
    }

    private fun scoreFromMetrics(
        metrics: BiomechanicalMetrics,
        issues: List<FormIssue>
    ): Triple<Int, Int, Int> {
        val techniquePenalty = issues.sumOf { issue ->
            when (issue.severity) {
                Severity.MINOR -> 5
                Severity.MODERATE -> 12
                Severity.MAJOR -> 22
            }
        }

        val technique = (100 - techniquePenalty).coerceIn(0, 100)

        val elbowDelta = abs(metrics.elbowAngleDeg - 90f).coerceAtMost(45f)
        val kneeDelta = abs(metrics.kneeAngleDeg - 125f).coerceAtMost(45f)
        val style = (100 - (elbowDelta + kneeDelta).toInt()).coerceIn(0, 100)

        val alignmentBonus = (
                1f - (metrics.elbowAlignmentOffset * 8f).coerceIn(0f, 1f)
                ) * 35f
        val heightBonus = (metrics.releaseHeightRatio * 4f).coerceIn(0f, 1f) * 30f
        val followThroughBonus = (
                (180f - metrics.wristFollowThroughDeg).coerceIn(0f, 110f) / 110f
                ) * 35f

        val accuracy = (
                alignmentBonus + heightBonus + followThroughBonus
                ).toInt().coerceIn(0, 100)

        return Triple(accuracy, style, technique)
    }

    private fun buildSuggestions(issues: List<FormIssue>): List<String> {
        if (issues.isEmpty()) {
            return listOf(
                "Form is consistent in the captured sequence.",
                "Keep the same setup and follow-through on every free throw."
            )
        }

        return issues.map { it.message } +
                "Use the BEEF cue: Balance, Eyes, Elbow, Follow-through."
    }

    private data class JointSet(
        val shoulder: PoseLandmark,
        val elbow: PoseLandmark,
        val wrist: PoseLandmark,
        val hip: PoseLandmark,
        val knee: PoseLandmark,
        val ankle: PoseLandmark,
        val oppositeAnkle: PoseLandmark
    )
}