package com.bramble.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder screens. Each will move to its own file once it has real content.

@Composable
fun HomeScreen() = PlaceholderScreen("Home")

@Composable
fun CalendarScreen() = PlaceholderScreen("Calendar")

@Composable
fun StoreScreen() = PlaceholderScreen("Store")

@Composable
fun AccountScreen() = PlaceholderScreen("Account")

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
