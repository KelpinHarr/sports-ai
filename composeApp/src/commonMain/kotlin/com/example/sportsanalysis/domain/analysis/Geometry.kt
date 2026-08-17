package com.example.sportsanalysis.domain.analysis

import com.example.sportsanalysis.domain.model.PoseLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

internal object Geometry {
    fun angleDeg(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Float {
        val abx = a.x - b.x; val aby = a.y - b.y
        val cbx = c.x - b.x; val cby = c.y - b.y
        val dot = abx * cbx + aby * cby
        val magAb = sqrt(abx * abx + aby * aby)
        val magCb = sqrt(cbx * cbx + cby * cby)
        if (magAb == 0f || magCb == 0f) return 0f
        val cos = (dot / (magAb * magCb)).coerceIn(-1f, 1f)
        return (acos(cos) * 180f / kotlin.math.PI).toFloat()
    }

    fun horizontalOffset(a: PoseLandmark, b: PoseLandmark): Float = abs(a.x - b.x)

    fun distance(a: PoseLandmark, b: PoseLandmark): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
