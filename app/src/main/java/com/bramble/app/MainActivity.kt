package com.bramble.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.bramble.app.data.ThemeSettings
import com.bramble.app.ui.BrambleApp
import com.bramble.app.ui.theme.BrambleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSettings.load(this)
        enableEdgeToEdge()
        // Instead of inflating an XML layout, Compose builds the UI from @Composable functions.
        setContent {
            // Light unless the Settings switch has been turned on; the phone's own dark mode is ignored.
            val darkTheme = ThemeSettings.darkMode ?: false

            // Keep the status/navigation bar icons readable when the app's theme differs from the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            BrambleTheme(darkTheme = darkTheme) {
                BrambleApp()
            }
        }
    }
}
