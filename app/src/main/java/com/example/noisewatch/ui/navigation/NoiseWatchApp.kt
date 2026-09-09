package com.example.noisewatch.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.noisewatch.ui.screens.HistoryScreen
import com.example.noisewatch.ui.screens.MeasureScreen
import com.example.noisewatch.ui.screens.SettingsScreen

@Composable
fun NoiseWatchApp(
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(NavDestination.MEASURE) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(text = destination.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        when (currentDestination) {
            NavDestination.MEASURE -> MeasureScreen(modifier = screenModifier)
            NavDestination.HISTORY -> HistoryScreen(modifier = screenModifier)
            NavDestination.SETTINGS -> SettingsScreen(modifier = screenModifier)
        }
    }
}
