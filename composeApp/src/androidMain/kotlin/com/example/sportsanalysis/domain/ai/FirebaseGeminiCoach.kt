package com.example.sportsanalysis.domain.ai

import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

internal class FirebaseGeminiCoach : AiCoach {
    private val model = Firebase
        .ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-3.7-flash")

    override suspend fun createFeedback(analysis: FreeThrowAnalysis): String? {
        val metrics = analysis.metrics
        val issueText = if (analysis.issues.isEmpty()) {
            "No rule-based issues detected."
        } else {
            analysis.issues.joinToString("\n") { issue ->
                "- ${issue.area}: ${issue.severity}: ${issue.message}"
            }
        }

        val prompt = """
            You are a basketball shooting coach specializing in free-throw biomechanics.
            Analyze only the measured data below. Do not claim whether the shot was made or missed because ball tracking is not provided.
            Give concise, practical feedback in 3 short bullet points and one priority drill.

            Scores:
            - Accuracy proxy: ${analysis.accuracyScore}/100
            - Style: ${analysis.styleScore}/100
            - Technique: ${analysis.techniqueScore}/100
            - Overall: ${analysis.overallScore}/100

            Biomechanics:
            - Elbow angle: ${metrics.elbowAngleDeg} degrees
            - Knee angle: ${metrics.kneeAngleDeg} degrees
            - Wrist follow-through angle: ${metrics.wristFollowThroughDeg} degrees
            - Elbow alignment offset: ${metrics.elbowAlignmentOffset}
            - Release height ratio: ${metrics.releaseHeightRatio}
            - Stance balance: ${metrics.stanceBalance}

            Detected issues:
            $issueText
        """.trimIndent()

        return model.generateContent(prompt).text?.trim()
    }
}
