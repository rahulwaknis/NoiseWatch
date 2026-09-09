package com.example.noisewatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedBrickRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onDeleteHistory: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete incident history?") },
            text = { Text(text = "This will permanently delete all saved NoiseWatch incidents from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MutedBrickRed
                    )
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(text = "Cancel")
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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Complaint Recipients
            SettingsSection(title = "Complaint Recipients") {
                RecipientRow(
                    name = "Pune Police Commissioner",
                    email = "punepolicecom@gmail.com",
                    icon = Icons.Default.Person
                )
                RecipientRow(
                    name = "MPCB",
                    email = "mpcb.nom@gmail.com",
                    icon = Icons.Default.Email
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { /* Add recipient placeholder */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = DeepNavyCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Add recipient",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DeepNavyCharcoal
                    )
                }
            }

            // Section 2: Measurement
            SettingsSection(title = "Measurement") {
                InfoRow(
                    icon = Icons.Default.GraphicEq,
                    text = "Reporting threshold: 75 dB(A)"
                )
                InfoRow(
                    icon = Icons.Default.Warning,
                    text = "Calibration: Not calibrated"
                )
                InfoRow(
                    icon = Icons.Default.Timer,
                    text = "Measurement duration: 60 seconds"
                )
            }

            // Section 3: Privacy
            SettingsSection(title = "Privacy") {
                ActionRow(
                    icon = Icons.Default.Lock,
                    text = "Manage permissions"
                )
                ActionRow(
                    icon = Icons.Default.Security,
                    text = "Delete incident history",
                    textColor = MutedBrickRed,
                    iconColor = MutedBrickRed,
                    onClick = { showDeleteDialog = true }
                )
            }

            // Section 4: About
            SettingsSection(title = "About") {
                ActionRow(
                    icon = Icons.Default.Info,
                    text = "Measurement disclaimer"
                )
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.Article,
                    text = "Noise pollution rules"
                )
                ActionRow(
                    icon = Icons.Default.Info,
                    text = "Privacy information"
                )
                InfoRow(
                    icon = Icons.Default.Info,
                    text = "App version 1.0"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DeepNavyCharcoal
        )
        content()
    }
}

@Composable
private fun RecipientRow(
    name: String,
    email: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepNavyCharcoal,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = DeepNavyCharcoal
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            tint = DeepNavyCharcoal,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepNavyCharcoal,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepNavyCharcoal
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    text: String,
    textColor: Color = DeepNavyCharcoal,
    iconColor: Color = DeepNavyCharcoal,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
