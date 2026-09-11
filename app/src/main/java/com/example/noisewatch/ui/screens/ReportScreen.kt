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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedAmberGold
import com.example.noisewatch.ui.theme.PaleSlateBlue
import com.example.noisewatch.ui.viewmodel.IncidentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    incidentId: Long,
    incidentViewModel: IncidentViewModel,
    onBackToMeasure: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToMeasure()
    }

    val incidentState by incidentViewModel.getIncidentById(incidentId).collectAsState(initial = null)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Noise Incident",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepNavyCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMeasure) {
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
        val incident = incidentState
        if (incident == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepNavyCharcoal)
            }
        } else {
            val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()) }
            val formattedDate = remember(incident.createdAt) { dateFormatter.format(Date(incident.createdAt)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PaleSlateBlue.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DeepNavyCharcoal,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Incident saved",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyCharcoal
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

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
                                text = String.format(Locale.US, "%.1f dB(A)", incident.laeq),
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
                                    text = String.format(Locale.US, "%.1f", incident.maximumDb),
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
                                    text = String.format(Locale.US, "%.1f", incident.minimumDb),
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
                                    text = "${incident.measurementDurationSeconds} sec",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepNavyCharcoal
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DeepNavyCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                    }

                    val hasCoords = incident.latitude != null && incident.longitude != null
                    val primaryLocationText = incident.locality
                        ?: incident.readableAddress
                        ?: if (hasCoords) String.format(Locale.US, "%.4f, %.4f", incident.latitude, incident.longitude) else "Location not captured"

                    Text(
                        text = primaryLocationText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasCoords) FontWeight.Medium else FontWeight.Normal,
                        color = DeepNavyCharcoal
                    )

                    if (hasCoords) {
                        val subText = buildList {
                            if (incident.locality != null || incident.readableAddress != null) {
                                add(String.format(Locale.US, "%.4f, %.4f", incident.latitude, incident.longitude))
                            }
                            if (incident.locationAccuracyMeters != null) {
                                add("Accuracy ±${incident.locationAccuracyMeters.roundToInt()} m")
                            }
                        }.joinToString("  •  ")

                        if (subText.isNotEmpty()) {
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Noise Source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyCharcoal
                    )
                    Text(
                        text = incident.noiseSource ?: "Not specified",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepNavyCharcoal
                    )
                }

                if (incident.notes != null) {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                        Text(
                            text = incident.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepNavyCharcoal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onBackToMeasure,
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
                        text = "BACK TO MEASURE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "VIEW HISTORY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyCharcoal
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Disabled placeholder */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "EMAIL COMPLAINT")
                    }

                    OutlinedButton(
                        onClick = { /* Disabled placeholder */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "Share on X")
                    }

                    OutlinedButton(
                        onClick = { /* Disabled placeholder */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = "Share PDF")
                    }
                }
            }
        }
    }
}
