package com.bramble.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bramble.app.ui.BrambleApp
import com.bramble.app.ui.theme.BrambleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Instead of inflating an XML layout, Compose builds the UI from @Composable functions.
        setContent {
            BrambleTheme {
                BrambleApp()
            }
        }
    }
}
