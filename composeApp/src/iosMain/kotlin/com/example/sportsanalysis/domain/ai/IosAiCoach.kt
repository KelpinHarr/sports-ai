package com.example.sportsanalysis.domain.ai

import com.example.sportsanalysis.domain.model.FreeThrowAnalysis

internal class IosAiCoach : AiCoach {
    override suspend fun createFeedback(analysis: FreeThrowAnalysis): String? = null
}