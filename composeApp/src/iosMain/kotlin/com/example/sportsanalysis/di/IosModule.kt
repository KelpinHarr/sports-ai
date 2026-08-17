package com.example.sportsanalysis.di

import com.example.sportsanalysis.domain.pose.PoseDetectorFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { PoseDetectorFactory() }
}