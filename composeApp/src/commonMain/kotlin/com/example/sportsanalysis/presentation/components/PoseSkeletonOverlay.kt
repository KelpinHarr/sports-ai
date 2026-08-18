package com.example.sportsanalysis.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.sportsanalysis.domain.model.PoseFrame
import com.example.sportsanalysis.domain.model.PoseLandmarkIndex as L

@Composable
fun PoseSkeletonOverlay(
    frame: PoseFrame?,
    highlighted: Set<Int>,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    val landmarks = frame?.landmarks ?: return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun pt(idx: Int): Offset? {
            val landmark = landmarks.getOrNull(idx) ?: return null

            if (landmark.visibility < 0.5f) {
                return null
            }

            val x = if (mirror) {
                1f - landmark.x
            } else {
                landmark.x
            }

            return Offset(
                x = x * w,
                y = landmark.y * h
            )
        }

        fun bone(a: Int, b: Int) {
            val p1 = pt(a) ?: return
            val p2 = pt(b) ?: return

            val color =
                if (a in highlighted || b in highlighted) {
                    Color(0xFFE53935)
                } else {
                    Color(0xFF00E539)
                }

            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 6f
            )
        }

        bone(L.LEFT_SHOULDER, L.RIGHT_SHOULDER)

        bone(L.LEFT_SHOULDER, L.LEFT_HIP)
        bone(L.RIGHT_SHOULDER, L.RIGHT_HIP)

        bone(L.LEFT_HIP, L.RIGHT_HIP)

        bone(L.LEFT_SHOULDER, L.LEFT_ELBOW)
        bone(L.LEFT_ELBOW, L.LEFT_WRIST)

        bone(L.RIGHT_SHOULDER, L.RIGHT_ELBOW)
        bone(L.RIGHT_ELBOW, L.RIGHT_WRIST)

        bone(L.LEFT_WRIST, L.LEFT_INDEX)
        bone(L.RIGHT_WRIST, L.RIGHT_INDEX)

        bone(L.LEFT_WRIST, L.LEFT_THUMB)
        bone(L.RIGHT_WRIST, L.RIGHT_THUMB)

        bone(L.LEFT_HIP, L.LEFT_KNEE)
        bone(L.LEFT_KNEE, L.LEFT_ANKLE)

        bone(L.RIGHT_HIP, L.RIGHT_KNEE)
        bone(L.RIGHT_KNEE, L.RIGHT_ANKLE)

        bone(L.LEFT_ANKLE, L.LEFT_HEEL)
        bone(L.LEFT_HEEL, L.LEFT_FOOT_INDEX)

        bone(L.RIGHT_ANKLE, L.RIGHT_HEEL)
        bone(L.RIGHT_HEEL, L.RIGHT_FOOT_INDEX)

        val points = listOf(
            L.LEFT_SHOULDER,
            L.RIGHT_SHOULDER,

            L.LEFT_ELBOW,
            L.RIGHT_ELBOW,

            L.LEFT_WRIST,
            L.RIGHT_WRIST,

            L.LEFT_INDEX,
            L.RIGHT_INDEX,

            L.LEFT_HIP,
            L.RIGHT_HIP,

            L.LEFT_KNEE,
            L.RIGHT_KNEE,

            L.LEFT_ANKLE,
            L.RIGHT_ANKLE,

            L.LEFT_HEEL,
            L.RIGHT_HEEL,

            L.LEFT_FOOT_INDEX,
            L.RIGHT_FOOT_INDEX
        )

        points.forEach { idx ->
            pt(idx)?.let { point ->

                val color =
                    if (idx in highlighted) {
                        Color(0xFFE53935)
                    } else {
                        Color.White
                    }

                drawCircle(
                    color = color,
                    radius = 8f,
                    center = point
                )
            }
        }
    }
}