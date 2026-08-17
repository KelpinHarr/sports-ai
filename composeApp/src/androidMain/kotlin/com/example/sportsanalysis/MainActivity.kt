package com.example.sportsanalysis

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var permissionGranted by remember { mutableStateOf(false) }
            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted -> permissionGranted = granted }

            LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }

            if (permissionGranted) {
                App()
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required to analyse your shot.")
                }
            }
        }
    }
}