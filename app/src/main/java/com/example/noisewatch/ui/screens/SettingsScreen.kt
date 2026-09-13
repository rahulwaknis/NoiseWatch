package com.example.noisewatch.ui.screens

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noisewatch.data.ComplaintPreferences
import com.example.noisewatch.data.ComplaintRecipient
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedBrickRed
import com.example.noisewatch.ui.theme.PaleSlateBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onDeleteHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val complaintPrefs = remember { ComplaintPreferences(context) }

    var recipients by remember { mutableStateOf(complaintPrefs.getRecipients()) }
    var defaultSubject by remember { mutableStateOf(complaintPrefs.getDefaultSubject()) }
    var defaultBody by remember { mutableStateOf(complaintPrefs.getDefaultBody()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRecipientDialog by remember { mutableStateOf(false) }
    var editingRecipient by remember { mutableStateOf<ComplaintRecipient?>(null) }

    var recipientNameInput by remember { mutableStateOf("") }
    var recipientEmailInput by remember { mutableStateOf("") }
    var recipientEmailError by remember { mutableStateOf<String?>(null) }

    var showSubjectDialog by remember { mutableStateOf(false) }
    var subjectInput by remember { mutableStateOf("") }

    var showBodyDialog by remember { mutableStateOf(false) }
    var bodyInput by remember { mutableStateOf("") }

    // Delete History Dialog
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedBrickRed)
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Add / Edit Recipient Dialog
    if (showRecipientDialog) {
        AlertDialog(
            onDismissRequest = { showRecipientDialog = false },
            title = {
                Text(text = if (editingRecipient == null) "Add Recipient" else "Edit Recipient")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = recipientNameInput,
                        onValueChange = { recipientNameInput = it },
                        label = { Text("Name / Authority (e.g., Police)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = recipientEmailInput,
                        onValueChange = {
                            recipientEmailInput = it
                            recipientEmailError = null
                        },
                        label = { Text("Email address") },
                        isError = recipientEmailError != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (recipientEmailError != null) {
                        Text(
                            text = recipientEmailError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val emailTrimmed = recipientEmailInput.trim()
                        if (emailTrimmed.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
                            recipientEmailError = "Please enter a valid email address."
                            return@TextButton
                        }

                        val updatedList = recipients.toMutableList()
                        if (editingRecipient == null) {
                            updatedList.add(
                                ComplaintRecipient(
                                    id = System.currentTimeMillis().toString(),
                                    name = recipientNameInput.trim().ifBlank { "Authority" },
                                    email = emailTrimmed
                                )
                            )
                        } else {
                            val index = updatedList.indexOfFirst { it.id == editingRecipient!!.id }
                            if (index != -1) {
                                updatedList[index] = ComplaintRecipient(
                                    id = editingRecipient!!.id,
                                    name = recipientNameInput.trim().ifBlank { "Authority" },
                                    email = emailTrimmed
                                )
                            }
                        }
                        recipients = updatedList
                        complaintPrefs.saveRecipients(updatedList)
                        showRecipientDialog = false
                    }
                ) {
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipientDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Edit Subject Dialog
    if (showSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text("Default Email Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Placeholders: {date}, {time}, {location}, {laeq}, {max}, {min}, {duration}, {source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Subject template") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = subjectInput.trim()
                        if (trimmed.isNotBlank()) {
                            defaultSubject = trimmed
                            complaintPrefs.saveDefaultSubject(trimmed)
                        }
                        showSubjectDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Body Dialog
    if (showBodyDialog) {
        AlertDialog(
            onDismissRequest = { showBodyDialog = false },
            title = { Text("Default Email Body") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Placeholders: {date}, {time}, {location}, {laeq}, {max}, {min}, {duration}, {source}, {notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = bodyInput,
                        onValueChange = { bodyInput = it },
                        label = { Text("Body template") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = bodyInput.trim()
                        if (trimmed.isNotBlank()) {
                            defaultBody = trimmed
                            complaintPrefs.saveDefaultBody(trimmed)
                        }
                        showBodyDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBodyDialog = false }) {
                    Text("Cancel")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Complaint Recipients
            SettingsCard(title = "Complaint Recipients") {
                if (recipients.isEmpty()) {
                    Text(
                        text = "No saved recipients. Add recipient email addresses below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    recipients.forEach { recipient ->
                        RecipientRow(
                            name = recipient.name,
                            email = recipient.email,
                            icon = Icons.Default.Person,
                            onEdit = {
                                editingRecipient = recipient
                                recipientNameInput = recipient.name
                                recipientEmailInput = recipient.email
                                recipientEmailError = null
                                showRecipientDialog = true
                            },
                            onDelete = {
                                val updatedList = recipients.filterNot { it.id == recipient.id }
                                recipients = updatedList
                                complaintPrefs.saveRecipients(updatedList)
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingRecipient = null
                            recipientNameInput = ""
                            recipientEmailInput = ""
                            recipientEmailError = null
                            showRecipientDialog = true
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = DeepNavyCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "+ Add recipient",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DeepNavyCharcoal
                    )
                }
            }

            // Card 2: Email Templates
            SettingsCard(title = "Email Templates") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            subjectInput = defaultSubject
                            showSubjectDialog = true
                        }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default email subject",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = DeepNavyCharcoal
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subject",
                            tint = DeepNavyCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = defaultSubject,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            bodyInput = defaultBody
                            showBodyDialog = true
                        }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default email body",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = DeepNavyCharcoal
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Body",
                            tint = DeepNavyCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = defaultBody.lines().firstOrNull() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Card 3: Measurement
            SettingsCard(title = "Measurement") {
                MeasurementInfoRow(
                    label = "Reporting threshold",
                    value = "75 dB(A)"
                )
                MeasurementInfoRow(
                    label = "Calibration",
                    value = "Not calibrated"
                )
                MeasurementInfoRow(
                    label = "Measurement duration",
                    value = "60 seconds"
                )
            }

            // Card 4: Privacy
            SettingsCard(title = "Privacy") {
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

            // Card 5: About
            SettingsCard(title = "About") {
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
                MeasurementInfoRow(
                    label = "App version",
                    value = "1.0"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PaleSlateBlue.copy(alpha = 0.35f),
            contentColor = DeepNavyCharcoal
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = DeepNavyCharcoal,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun RecipientRow(
    name: String,
    email: String,
    icon: ImageVector,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = DeepNavyCharcoal,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MutedBrickRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MeasurementInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepNavyCharcoal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
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
