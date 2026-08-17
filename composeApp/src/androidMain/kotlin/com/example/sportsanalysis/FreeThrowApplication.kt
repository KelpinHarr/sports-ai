package com.example.sportsanalysis

import android.app.Application
import com.example.sportsanalysis.di.initKoin
import org.koin.android.ext.koin.androidContext

class FreeThrowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@FreeThrowApplication)
        }
    }
}