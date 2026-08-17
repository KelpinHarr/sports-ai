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
    mirror: Boolean = true
) {
    val landmarks = frame?.landmarks ?: return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun pt(idx: Int): Offset? {
            val l = landmarks.getOrNull(idx) ?: return null
            if (l.visibility < 0.5f) return null
            val x = if (mirror) (1f - l.x) else l.x
            return Offset(x * w, l.y * h)
        }

        fun bone(a: Int, b: Int) {
            val p1 = pt(a) ?: return
            val p2 = pt(b) ?: return
            val color = if (a in highlighted || b in highlighted) Color(0xFFE53935) else Color(0xFF43A047)
            drawLine(color, p1, p2, strokeWidth = 6f)
        }

        bone(L.LEFT_SHOULDER, L.RIGHT_SHOULDER)
        bone(L.LEFT_HIP, L.RIGHT_HIP)
        bone(L.LEFT_SHOULDER, L.LEFT_HIP)
        bone(L.RIGHT_SHOULDER, L.RIGHT_HIP)

        bone(L.LEFT_SHOULDER, L.LEFT_ELBOW); bone(L.LEFT_ELBOW, L.LEFT_WRIST)
        bone(L.RIGHT_SHOULDER, L.RIGHT_ELBOW); bone(L.RIGHT_ELBOW, L.RIGHT_WRIST)

        bone(L.LEFT_HIP, L.LEFT_KNEE); bone(L.LEFT_KNEE, L.LEFT_ANKLE)
        bone(L.RIGHT_HIP, L.RIGHT_KNEE); bone(L.RIGHT_KNEE, L.RIGHT_ANKLE)

        listOf<Int>(
            L.LEFT_SHOULDER, L.RIGHT_SHOULDER, L.LEFT_ELBOW, L.RIGHT_ELBOW,
            L.LEFT_WRIST, L.RIGHT_WRIST, L.LEFT_HIP, L.RIGHT_HIP,
            L.LEFT_KNEE, L.RIGHT_KNEE, L.LEFT_ANKLE, L.RIGHT_ANKLE
        ).forEach { idx ->
            pt(idx)?.let { p ->
                val color = if (idx in highlighted) Color(0xFFE53935) else Color.White
                drawCircle(color, radius = 8f, center = p)
            }
        }
    }
}