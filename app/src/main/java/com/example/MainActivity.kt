package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.MainAppShell
import com.example.ui.theme.GVJVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val safeSettings = settings ?: com.example.data.local.entity.SettingsEntity()

            GVJVaultTheme(
                paletteName = safeSettings.currentTheme,
                accentColorHex = safeSettings.accentColorHex
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppShell(viewModel = viewModel)
                }
            }
        }
    }
}
