package com.example.noisewatch.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noisewatch.ui.screens.HistoryScreen
import com.example.noisewatch.ui.screens.IncidentReviewScreen
import com.example.noisewatch.ui.screens.MeasureScreen
import com.example.noisewatch.ui.screens.MeasuringScreen
import com.example.noisewatch.ui.screens.ReportScreen
import com.example.noisewatch.ui.screens.SettingsScreen
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.NavIndicatorAmber
import com.example.noisewatch.ui.theme.WarmIvory
import com.example.noisewatch.ui.viewmodel.IncidentViewModel
import com.example.noisewatch.ui.viewmodel.MeasuringViewModel
import kotlinx.coroutines.launch

@Composable
fun NoiseWatchApp(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Measure) }
    val measuringViewModel: MeasuringViewModel = viewModel()
    val incidentViewModel: IncidentViewModel = viewModel()
    val incidentsHistory by incidentViewModel.allIncidents.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val showBottomBar = when (currentScreen) {
        Screen.Measure, Screen.History, Screen.Settings -> true
        Screen.Measuring, is Screen.IncidentReview, is Screen.Report -> false
    }

    val currentTopLevelDestination = when (currentScreen) {
        Screen.Measure -> NavDestination.MEASURE
        Screen.History -> NavDestination.HISTORY
        Screen.Settings -> NavDestination.SETTINGS
        else -> NavDestination.MEASURE
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmIvory,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = WarmIvory,
                    contentColor = DeepNavyCharcoal
                ) {
                    NavDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentTopLevelDestination == destination,
                            onClick = {
                                currentScreen = when (destination) {
                                    NavDestination.MEASURE -> Screen.Measure
                                    NavDestination.HISTORY -> Screen.History
                                    NavDestination.SETTINGS -> Screen.Settings
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepNavyCharcoal,
                                selectedTextColor = DeepNavyCharcoal,
                                indicatorColor = NavIndicatorAmber,
                                unselectedIconColor = DeepNavyCharcoal.copy(alpha = 0.6f),
                                unselectedTextColor = DeepNavyCharcoal.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (val screen = currentScreen) {
            Screen.Measure -> MeasureScreen(
                modifier = screenModifier,
                onStartMeasurement = { currentScreen = Screen.Measuring }
            )
            Screen.History -> HistoryScreen(
                modifier = screenModifier,
                incidents = incidentsHistory,
                onIncidentClick = { id ->
                    currentScreen = Screen.Report(id)
                }
            )
            Screen.Settings -> SettingsScreen(
                modifier = screenModifier,
                onDeleteHistory = {
                    coroutineScope.launch {
                        incidentViewModel.deleteAllIncidents()
                    }
                }
            )
            Screen.Measuring -> MeasuringScreen(
                modifier = screenModifier,
                viewModel = measuringViewModel,
                onMeasurementComplete = { measurementData ->
                    currentScreen = Screen.IncidentReview(measurementData)
                },
                onCancelMeasurement = {
                    currentScreen = Screen.Measure
                }
            )
            is Screen.IncidentReview -> IncidentReviewScreen(
                modifier = screenModifier,
                measurement = screen.measurement,
                incidentViewModel = incidentViewModel,
                onIncidentSaved = { savedId ->
                    currentScreen = Screen.Report(savedId)
                },
                onCancelReview = {
                    currentScreen = Screen.Measure
                }
            )
            is Screen.Report -> ReportScreen(
                modifier = screenModifier,
                incidentId = screen.incidentId,
                incidentViewModel = incidentViewModel,
                onBackToMeasure = {
                    currentScreen = Screen.Measure
                },
                onViewHistory = {
                    currentScreen = Screen.History
                }
            )
        }
    }
}
