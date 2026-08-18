package com.example.sportsanalysis.di

import com.example.sportsanalysis.domain.ai.AiCoach
import com.example.sportsanalysis.domain.ai.FirebaseGeminiCoach
import com.example.sportsanalysis.domain.pose.AndroidPoseDetector
import com.example.sportsanalysis.domain.pose.PoseDetector
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PoseDetector> {
        AndroidPoseDetector(androidContext())
    }

    single<AiCoach> {
        FirebaseGeminiCoach()
    }
}