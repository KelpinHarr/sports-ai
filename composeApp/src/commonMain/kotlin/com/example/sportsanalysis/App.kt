package com.example.sportsanalysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sportsanalysis.presentation.screens.AnalysisScreen
import com.example.sportsanalysis.presentation.screens.HistoryScreen
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "analyze"
        ) {
            composable("analyze") {
                AnalysisScreen(
                    onOpenHistory = { navController.navigate("history") }
                )
            }

            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}