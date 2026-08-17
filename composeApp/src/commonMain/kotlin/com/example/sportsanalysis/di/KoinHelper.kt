package com.example.sportsanalysis.di

import com.example.sportsanalysis.domain.pose.PoseDetector
import org.koin.mp.KoinPlatform


object KoinHelper {
    fun providePoseDetector(): PoseDetector =
        KoinPlatform.getKoin().get()
}