package com.bramble.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bramble.app.R
import com.bramble.app.ui.screens.AccountScreen
import com.bramble.app.ui.screens.CalendarScreen
import com.bramble.app.ui.screens.HomeScreen
import com.bramble.app.ui.screens.SettingsScreen
import com.bramble.app.ui.screens.StoreScreen
import com.bramble.app.ui.theme.BrambleTheme

/** The four bottom-bar destinations, in display order. */
enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    Home(R.string.tab_home, R.drawable.ic_home),
    Calendar(R.string.tab_calendar, R.drawable.ic_calendar),
    Store(R.string.tab_store, R.drawable.ic_store),
    Account(R.string.tab_account, R.drawable.ic_account),
}

@Composable
fun BrambleApp() {
    // rememberSaveable keeps the selected tab across screen rotation and process death.
    var currentTab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            // Sage bar with a lavender selection pill (light) / deep green bar with a purple pill (dark).
            NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = {
                            currentTab = tab
                            showSettings = false
                        },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (showSettings) {
                SettingsScreen(onBack = { showSettings = false })
            } else {
                when (currentTab) {
                    Tab.Home -> HomeScreen()
                    Tab.Calendar -> CalendarScreen()
                    Tab.Store -> StoreScreen()
                    Tab.Account -> AccountScreen()
                }
                // Menu button sits in the top-left corner of every tab and opens settings.
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrambleAppPreview() {
    BrambleTheme { BrambleApp() }
}