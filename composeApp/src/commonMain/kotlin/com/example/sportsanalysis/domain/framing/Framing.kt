package com.example.sportsanalysis.domain.framing

import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmark
import com.example.sportsanalysis.domain.model.PoseLandmarkIndex

enum class FramingStatus {
    READY,
    TOO_FAR,
    TOO_CLOSE,
    MOVE_LEFT,
    MOVE_RIGHT,
    BODY_NOT_VISIBLE,
    NEED_MORE_HEADROOM
}

data class FramingResult(
    val status: FramingStatus,
    val message: String
) {
    val isReady: Boolean
        get() = status == FramingStatus.READY
}

fun evaluateFraming(frame: PoseFrame): FramingResult {
    if (!hasRequiredLandmarks(frame)) {
        return FramingResult(
            status = FramingStatus.BODY_NOT_VISIBLE,
            message = "Keep your full body in frame"
        )
    }

    val nose = frame.landmark(PoseLandmarkIndex.NOSE)
        ?: return bodyNotVisible()

    val leftShoulder = frame.landmark(PoseLandmarkIndex.LEFT_SHOULDER)
        ?: return bodyNotVisible()

    val rightShoulder = frame.landmark(PoseLandmarkIndex.RIGHT_SHOULDER)
        ?: return bodyNotVisible()

    val leftHip = frame.landmark(PoseLandmarkIndex.LEFT_HIP)
        ?: return bodyNotVisible()

    val rightHip = frame.landmark(PoseLandmarkIndex.RIGHT_HIP)
        ?: return bodyNotVisible()

    val leftAnkle = frame.landmark(PoseLandmarkIndex.LEFT_ANKLE)
        ?: return bodyNotVisible()

    val rightAnkle = frame.landmark(PoseLandmarkIndex.RIGHT_ANKLE)
        ?: return bodyNotVisible()

    val ankleY = (leftAnkle.y + rightAnkle.y) / 2f

    val bodyHeight = ankleY - nose.y

    if (bodyHeight <= 0f) {
        return bodyNotVisible()
    }

    val shoulderCenterX =
        (leftShoulder.x + rightShoulder.x) / 2f

    val hipCenterX =
        (leftHip.x + rightHip.x) / 2f

    val bodyCenterX =
        (shoulderCenterX + hipCenterX) / 2f

    val availableHeadroom = nose.y

    // Extra room required for the shooting arm to extend above the head.
    // This automatically scales with the person's apparent height.
    val requiredHeadroom = bodyHeight * HEADROOM_RATIO

    return when {
        bodyHeight < MIN_BODY_HEIGHT -> {
            FramingResult(
                status = FramingStatus.TOO_FAR,
                message = "Move slightly closer"
            )
        }

        bodyHeight > MAX_BODY_HEIGHT -> {
            FramingResult(
                status = FramingStatus.TOO_CLOSE,
                message = "Move farther back for your shooting motion"
            )
        }

        availableHeadroom < requiredHeadroom -> {
            FramingResult(
                status = FramingStatus.NEED_MORE_HEADROOM,
                message = "Move farther back so your raised arm stays in frame"
            )
        }

        leftAnkle.y > BOTTOM_LIMIT ||
                rightAnkle.y > BOTTOM_LIMIT -> {
            FramingResult(
                status = FramingStatus.TOO_CLOSE,
                message = "Move farther back so your feet stay in frame"
            )
        }

        bodyCenterX < MIN_CENTER_X -> {
            FramingResult(
                status = FramingStatus.MOVE_RIGHT,
                message = "Move slightly to the right"
            )
        }

        bodyCenterX > MAX_CENTER_X -> {
            FramingResult(
                status = FramingStatus.MOVE_LEFT,
                message = "Move slightly to the left"
            )
        }

        else -> {
            FramingResult(
                status = FramingStatus.READY,
                message = "Ready"
            )
        }
    }
}

private fun hasRequiredLandmarks(
    frame: PoseFrame
): Boolean {
    val required = listOf(
        PoseLandmarkIndex.NOSE,
        PoseLandmarkIndex.LEFT_SHOULDER,
        PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_HIP,
        PoseLandmarkIndex.RIGHT_HIP,
        PoseLandmarkIndex.LEFT_KNEE,
        PoseLandmarkIndex.RIGHT_KNEE,
        PoseLandmarkIndex.LEFT_ANKLE,
        PoseLandmarkIndex.RIGHT_ANKLE
    )

    return required.all { index ->
        val landmark = frame.landmark(index)

        landmark != null &&
                landmark.visibility >= MIN_VISIBILITY &&
                landmark.x in 0f..1f &&
                landmark.y in 0f..1f
    }
}

private fun PoseFrame.landmark(
    index: Int
): PoseLandmark? {
    return landmarks.getOrNull(index)
}

private fun bodyNotVisible(): FramingResult {
    return FramingResult(
        status = FramingStatus.BODY_NOT_VISIBLE,
        message = "Keep your full body in frame"
    )
}

private const val MIN_VISIBILITY = 0.55f

private const val MIN_BODY_HEIGHT = 0.28f
private const val MAX_BODY_HEIGHT = 0.58f

private const val HEADROOM_RATIO = 0.30f

private const val MIN_CENTER_X = 0.32f
private const val MAX_CENTER_X = 0.68f

private const val BOTTOM_LIMIT = 0.95f