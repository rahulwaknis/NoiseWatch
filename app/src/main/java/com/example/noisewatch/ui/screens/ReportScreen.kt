package com.example.noisewatch.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.noisewatch.data.ComplaintPreferences
import com.example.noisewatch.export.PdfReportGenerator
import com.example.noisewatch.model.Incident
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedAmberGold
import com.example.noisewatch.ui.theme.PaleSlateBlue
import com.example.noisewatch.ui.theme.RestrainedAmber
import com.example.noisewatch.ui.theme.VeryPaleWarmAmber
import com.example.noisewatch.ui.viewmodel.IncidentViewModel
import com.example.noisewatch.util.TemplateMerger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class IncidentReportUiState(
    val id: Long,
    val formattedDateTime: String,
    val primaryLocation: String,
    val secondaryLocationDetails: String?,
    val hasCoordinates: Boolean,
    val laeqFormatted: String,
    val maxDbFormatted: String,
    val minDbFormatted: String,
    val durationFormatted: String,
    val isHighNoise: Boolean,
    val noiseSource: String?,
    val notes: String?,
    val photoUri: String?,
    val disclaimerText: String = "Phone measurements are indicative and are not certified enforcement measurements. Authorities may require verification using calibrated equipment."
)

fun Incident.toReportUiState(): IncidentReportUiState {
    val dateFormatter = SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())
    val dateStr = dateFormatter.format(Date(createdAt))

    val hasCoords = latitude != null && longitude != null
    val primaryLoc = locality
        ?: readableAddress
        ?: if (hasCoords) String.format(Locale.US, "%.4f, %.4f", latitude, longitude) else "Location not captured"

    val subLocList = mutableListOf<String>()
    if (locality != null || readableAddress != null) {
        if (hasCoords) {
            subLocList.add(String.format(Locale.US, "%.4f, %.4f", latitude, longitude))
        }
    }
    if (locationAccuracyMeters != null) {
        subLocList.add("Accuracy ±${locationAccuracyMeters.roundToInt()} m")
    }
    val secLoc = if (subLocList.isNotEmpty()) subLocList.joinToString("  •  ") else null

    return IncidentReportUiState(
        id = id,
        formattedDateTime = dateStr,
        primaryLocation = primaryLoc,
        secondaryLocationDetails = secLoc,
        hasCoordinates = hasCoords,
        laeqFormatted = String.format(Locale.US, "%.1f", laeq),
        maxDbFormatted = String.format(Locale.US, "%.1f", maximumDb),
        minDbFormatted = String.format(Locale.US, "%.1f", minimumDb),
        durationFormatted = "$measurementDurationSeconds sec",
        isHighNoise = laeq >= 75.0,
        noiseSource = noiseSource?.takeIf { it.isNotBlank() },
        notes = notes?.takeIf { it.isNotBlank() },
        photoUri = photoUri?.takeIf { it.isNotBlank() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    incidentId: Long,
    incidentViewModel: IncidentViewModel,
    onBackToMeasure: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isPreparingEmail by remember { mutableStateOf(false) }

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
                        text = "Incident Report",
                        style = MaterialTheme.typography.titleLarge,
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
            val reportState = remember(incident) { incident.toReportUiState() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Metadata Line
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = reportState.formattedDateTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = reportState.primaryLocation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyCharcoal
                    )
                }

                // Primary Measurement Summary Card
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
                                text = "${reportState.laeqFormatted} dB(A)",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyCharcoal,
                                lineHeight = 36.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "LAeq",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavyCharcoal
                            )
                        }

                        HorizontalDivider(color = DeepNavyCharcoal.copy(alpha = 0.15f))

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
                                    text = reportState.maxDbFormatted,
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
                                    text = reportState.minDbFormatted,
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
                                    text = reportState.durationFormatted,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepNavyCharcoal
                                )
                            }
                        }
                    }
                }

                // Threshold Context Card
                if (reportState.isHighNoise) {
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

                // Location Details Section
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

                    Text(
                        text = reportState.primaryLocation,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (reportState.hasCoordinates) FontWeight.Medium else FontWeight.Normal,
                        color = DeepNavyCharcoal
                    )

                    if (reportState.secondaryLocationDetails != null) {
                        Text(
                            text = reportState.secondaryLocationDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Noise Source Section (only shown if present)
                if (reportState.noiseSource != null) {
                    HorizontalDivider(color = DeepNavyCharcoal.copy(alpha = 0.15f))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Noise Source",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                        Text(
                            text = reportState.noiseSource,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DeepNavyCharcoal
                        )
                    }
                }

                // Notes Section (only shown if present)
                if (reportState.notes != null) {
                    HorizontalDivider(color = DeepNavyCharcoal.copy(alpha = 0.15f))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                        Text(
                            text = reportState.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepNavyCharcoal
                        )
                    }
                }

                // Photo Section (only shown if present)
                if (reportState.photoUri != null) {
                    HorizontalDivider(color = DeepNavyCharcoal.copy(alpha = 0.15f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                        AsyncImage(
                            model = reportState.photoUri,
                            contentDescription = "Saved incident photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Measurement Disclaimer
                Text(
                    text = reportState.disclaimerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Action 1: EMAIL COMPLAINT
                Button(
                    onClick = {
                        if (isPreparingEmail || isGeneratingPdf) return@Button
                        launchEmailComplaint(context, reportState, coroutineScope) { isPreparingEmail = it }
                    },
                    enabled = !isPreparingEmail && !isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedAmberGold,
                        contentColor = DeepNavyCharcoal
                    )
                ) {
                    if (isPreparingEmail) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DeepNavyCharcoal,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "EMAIL COMPLAINT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Secondary Action 2: SHARE REPORT (Generates PDF & opens Android Share Sheet)
                OutlinedButton(
                    onClick = {
                        if (isGeneratingPdf || isPreparingEmail) return@OutlinedButton
                        isGeneratingPdf = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val pdfFile = PdfReportGenerator.generatePdfReport(context, reportState)
                            isGeneratingPdf = false
                            if (pdfFile != null) {
                                PdfReportGenerator.sharePdfReport(context, pdfFile)
                            }
                        }
                    },
                    enabled = !isGeneratingPdf && !isPreparingEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DeepNavyCharcoal,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SHARE REPORT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyCharcoal
                            )
                        }
                    }
                }

                // Secondary Action 3: SHARE ON X
                OutlinedButton(
                    onClick = {
                        launchShareOnX(context, reportState)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SHARE ON X",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyCharcoal
                        )
                    }
                }

                // Navigation Actions
                OutlinedButton(
                    onClick = onBackToMeasure,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "BACK TO MEASURE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyCharcoal
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
            }
        }
    }
}

private fun launchEmailComplaint(
    context: Context,
    reportState: IncidentReportUiState,
    coroutineScope: CoroutineScope,
    onLoadingChange: (Boolean) -> Unit
) {
    onLoadingChange(true)
    coroutineScope.launch(Dispatchers.IO) {
        val pdfFile = PdfReportGenerator.generatePdfReport(context, reportState)
        onLoadingChange(false)

        if (pdfFile != null) {
            val complaintPrefs = ComplaintPreferences(context)
            val recipients = complaintPrefs.getRecipients()
            val subjectTemplate = complaintPrefs.getDefaultSubject()
            val bodyTemplate = complaintPrefs.getDefaultBody()

            val mergedSubject = TemplateMerger.mergeTemplate(subjectTemplate, reportState)
            val mergedBody = TemplateMerger.mergeTemplate(bodyTemplate, reportState)

            val recipientEmails = recipients.map { it.email }.toTypedArray()

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.example.noisewatch.fileprovider",
                pdfFile.canonicalFile
            )

            Handler(Looper.getMainLooper()).post {
                try {
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        if (recipientEmails.isNotEmpty()) {
                            putExtra(Intent.EXTRA_EMAIL, recipientEmails)
                        }
                        putExtra(Intent.EXTRA_SUBJECT, mergedSubject)
                        putExtra(Intent.EXTRA_TEXT, mergedBody)
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val chooser = Intent.createChooser(emailIntent, "Send complaint email")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (_: Exception) {
                    Toast.makeText(context, "No email app is available on this device.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to generate report PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun launchShareOnX(
    context: Context,
    reportState: IncidentReportUiState
) {
    val text = TemplateMerger.generateXShareText(reportState)
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, "Share on X")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to share on X.", Toast.LENGTH_SHORT).show()
    }
}
