package com.personal.spese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.personal.spese.core.model.ThemeMode
import com.personal.spese.navigation.AppRoot
import com.personal.spese.ui.theme.SpeseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SpeseApp).container
        setContent {
            val themeMode by container.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            SpeseTheme(themeMode = themeMode) {
                AppRoot()
            }
        }
    }
}
