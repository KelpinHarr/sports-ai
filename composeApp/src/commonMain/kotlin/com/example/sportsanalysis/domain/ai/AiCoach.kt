package com.example.sportsanalysis.domain.ai

import com.example.sportsanalysis.domain.model.FreeThrowAnalysis

interface AiCoach {
    suspend fun createFeedback(analysis: FreeThrowAnalysis): String?
}