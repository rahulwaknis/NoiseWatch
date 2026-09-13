package com.example.noisewatch.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noisewatch.model.MeasurementData
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedAmberGold
import com.example.noisewatch.ui.theme.MutedBrickRed
import com.example.noisewatch.ui.theme.PaleSlateBlue
import com.example.noisewatch.ui.viewmodel.MeasuringViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasuringScreen(
    viewModel: MeasuringViewModel,
    onMeasurementComplete: (MeasurementData) -> Unit,
    onCancelMeasurement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val state by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var hasHandledCompletion by rememberSaveable { mutableStateOf(false) }

    // Keep screen awake ONLY while MeasuringScreen is in composition
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.cancelMeasurement()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startMeasurement()
    }

    BackHandler {
        showCancelDialog = true
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished && !hasHandledCompletion) {
            hasHandledCompletion = true
            val data = viewModel.stopMeasurement()
            onMeasurementComplete(data)
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(text = "Cancel this measurement?")
            },
            text = {
                Text(text = "The current measurement will be discarded.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelMeasurement()
                        onCancelMeasurement()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MutedBrickRed
                    )
                ) {
                    Text(
                        text = "Discard",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false }
                ) {
                    Text(
                        text = "Keep measuring",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Measuring noise",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepNavyCharcoal
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Big Measurement Number
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.0f", state.currentDb),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavyCharcoal,
                    lineHeight = 80.sp
                )
            }

            // Thin Vertical Bar Sound Waveform
            ThinBarSoundVisualizer(
                history = state.history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            // Side-by-side Analytical Cards (LAeq and Maximum)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = PaleSlateBlue,
                        contentColor = DeepNavyCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LAeq",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavyCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f dB(A)", state.laeq),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = PaleSlateBlue,
                        contentColor = DeepNavyCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Maximum",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepNavyCharcoal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f dB(A)", state.maxDb),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                    }
                }
            }

            // Elapsed Time
            Text(
                text = String.format(Locale.US, "%d:%02d / 1:00", state.elapsedSeconds / 60, state.elapsedSeconds % 60),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = DeepNavyCharcoal
            )

            if (state.hasClipped) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Microphone input may have clipped at this noise level.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Thumb Zone Action Area: FINISH MEASUREMENT and Cancel measurement
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: FINISH MEASUREMENT
                Button(
                    onClick = {
                        if (!hasHandledCompletion) {
                            hasHandledCompletion = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val data = viewModel.stopMeasurement()
                            onMeasurementComplete(data)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedAmberGold,
                        contentColor = DeepNavyCharcoal
                    )
                ) {
                    Text(
                        text = "FINISH MEASUREMENT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Action: Cancel measurement
                TextButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MutedBrickRed
                    )
                ) {
                    Text(
                        text = "Cancel measurement",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinBarSoundVisualizer(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    val barColor = PaleSlateBlue
    val activeBarColor = DeepNavyCharcoal

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxBars = 36
        val barWidth = 3.dp.toPx()
        val spacing = (width - (maxBars * barWidth)) / (maxBars - 1)

        val visibleHistory = history.takeLast(maxBars)
        val padCount = maxBars - visibleHistory.size

        for (i in 0 until maxBars) {
            val x = i * (barWidth + spacing) + (barWidth / 2f)
            val db = if (i < padCount) 40.0 else visibleHistory[i - padCount]
            val normalized = ((db - 30.0) / 70.0).coerceIn(0.1, 1.0).toFloat()
            val barHeight = normalized * (height * 0.85f)
            val yStart = (height - barHeight) / 2f
            val yEnd = yStart + barHeight

            drawLine(
                color = if (i >= padCount) activeBarColor.copy(alpha = 0.6f) else barColor.copy(alpha = 0.4f),
                start = Offset(x, yStart),
                end = Offset(x, yEnd),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
