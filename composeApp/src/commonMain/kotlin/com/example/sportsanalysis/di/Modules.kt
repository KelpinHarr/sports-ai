package com.example.sportsanalysis.di

import com.example.sportsanalysis.data.repository.AnalysisRepository
import com.example.sportsanalysis.domain.analysis.FreeThrowAnalyzer
import com.example.sportsanalysis.domain.pose.PoseDetectorFactory
import com.example.sportsanalysis.presentation.screens.AnalysisViewModel
import com.example.sportsanalysis.presentation.screens.HistoryViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val sharedModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }

    single { AnalysisRepository(get(), get()) }

    factory { FreeThrowAnalyzer() }

    factory { get<PoseDetectorFactory>().create() }

    viewModelOf(::AnalysisViewModel)
    viewModelOf(::HistoryViewModel)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModule, platformModule())
    }
}

expect fun platformModule(): Module