package com.example.noisewatch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noisewatch.model.Incident
import com.example.noisewatch.model.MeasurementData
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedAmberGold
import com.example.noisewatch.ui.theme.MutedBrickRed
import com.example.noisewatch.ui.theme.PaleSlateBlue
import com.example.noisewatch.ui.theme.RestrainedAmber
import com.example.noisewatch.ui.theme.VeryPaleWarmAmber
import com.example.noisewatch.ui.viewmodel.IncidentViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentReviewScreen(
    measurement: MeasurementData,
    incidentViewModel: IncidentViewModel,
    onIncidentSaved: (Long) -> Unit,
    onCancelReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onCancelReview()
    }

    val coroutineScope = rememberCoroutineScope()
    var selectedSource by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }

    val noiseSources = listOf(
        "DJ / amplified music",
        "Loudspeaker / public address system",
        "Religious event",
        "Procession",
        "Festival event",
        "Firecrackers",
        "Construction",
        "Industrial activity",
        "Vehicle horns",
        "Generator",
        "Other"
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(text = "Discard this incident?")
            },
            text = {
                Text(text = "Your measurement and incident details will not be saved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onCancelReview()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MutedBrickRed
                    )
                ) {
                    Text(text = "Discard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false }
                ) {
                    Text(text = "Keep editing")
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
                        text = "Incident Review",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavyCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelReview) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepNavyCharcoal
                        )
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Measurement complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            if (saveErrorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = saveErrorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Measurement Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PaleSlateBlue,
                    contentColor = DeepNavyCharcoal
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "LAeq",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepNavyCharcoal
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f dB(A)", measurement.laeq),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Maximum",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavyCharcoal
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", measurement.maxDb),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyCharcoal
                            )
                        }

                        Column {
                            Text(
                                text = "Minimum",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavyCharcoal
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", measurement.minDb),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyCharcoal
                            )
                        }

                        Column {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavyCharcoal
                            )
                            Text(
                                text = "${measurement.durationSeconds} sec",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyCharcoal
                            )
                        }
                    }
                }
            }

            // High Noise Card
            val isHighNoise = measurement.laeq >= 75.0
            if (isHighNoise) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = VeryPaleWarmAmber,
                        contentColor = DeepNavyCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = RestrainedAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "High noise level",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyCharcoal
                            )
                            Text(
                                text = "This reading exceeds the app's 75 dB(A) reporting threshold.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepNavyCharcoal
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PaleSlateBlue.copy(alpha = 0.5f),
                        contentColor = DeepNavyCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "This reading is below the app's simplified reporting threshold. Noise regulations may still apply depending on location, time and source.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = DeepNavyCharcoal
                    )
                }
            }

            if (measurement.hasClipped) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = VeryPaleWarmAmber
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
                            tint = RestrainedAmber
                        )
                        Text(
                            text = "Microphone input may have clipped at this noise level.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepNavyCharcoal
                        )
                    }
                }
            }

            // Compact Dropdown Noise Source Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSource ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Noise source (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        noiseSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source) },
                                onClick = {
                                    selectedSource = source
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Optional Notes Text Field
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(8.dp)
            )

            // Photo Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavyCharcoal
                )
                DashedPhotoTile()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Creating an incident only saves the report in NoiseWatch. Nothing is emailed, posted or shared automatically",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Primary CREATE INCIDENT button
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    saveErrorMessage = null
                    coroutineScope.launch {
                        try {
                            val incident = Incident(
                                measurementDurationSeconds = measurement.durationSeconds,
                                laeq = measurement.laeq,
                                maximumDb = measurement.maxDb,
                                minimumDb = measurement.minDb,
                                noiseSource = selectedSource,
                                notes = notesText.ifBlank { null }
                            )
                            val newId = incidentViewModel.saveIncident(incident)
                            isSaving = false
                            onIncidentSaved(newId)
                        } catch (e: Exception) {
                            isSaving = false
                            saveErrorMessage = "Failed to save incident: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedAmberGold,
                    contentColor = DeepNavyCharcoal
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DeepNavyCharcoal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "CREATE INCIDENT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = { showDiscardDialog = true },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MutedBrickRed
                )
            ) {
                Text(
                    text = "Discard incident",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DashedPhotoTile(
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val strokeWidth = 1.dp
    val dashWidth = 6.dp
    val gapWidth = 4.dp

    Box(
        modifier = modifier
            .size(width = 96.dp, height = 80.dp)
            .drawBehind {
                val strokePx = strokeWidth.toPx()
                val dashPx = dashWidth.toPx()
                val gapPx = gapWidth.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                val cornerRadius = 8.dp.toPx()
                drawRoundRect(
                    color = outlineColor,
                    style = Stroke(width = strokePx, pathEffect = pathEffect),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = DeepNavyCharcoal,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.labelSmall,
                color = DeepNavyCharcoal
            )
        }
    }
}
