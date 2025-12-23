package com.example.expencetrackerapp.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.ui.theme.*
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel, onNavigateToCategories: () -> Unit) {
    val context = LocalContext.current

    var smsPermissionGranted by rememberSaveable() { mutableStateOf(false) }
    var notificationPermissionGranted by rememberSaveable() { mutableStateOf(false) }
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }

    val smsImportState by viewModel.smsImportState.collectAsState()

    val smsPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                smsPermissionGranted =
                        permissions[Manifest.permission.RECEIVE_SMS] == true &&
                                permissions[Manifest.permission.READ_SMS] == true
            }

    val notificationPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
            ) { granted -> notificationPermissionGranted = granted }

    // Handle import result dialog
    LaunchedEffect(smsImportState) {
        when (smsImportState) {
            is ExpenseViewModel.SmsImportState.Success,
            is ExpenseViewModel.SmsImportState.Error -> {
                showImportDialog = true
            }
            else -> {}
        }
    }

    val currentThemeMode = ThemeState.themeMode

    Box(modifier = Modifier.fillMaxSize()) {
        // Background pattern
        com.example.expencetrackerapp.ui.components.BackgroundPattern()

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Appearance Section
            item { SectionHeader(text = "Appearance", color = MaterialTheme.colorScheme.primary) }

            item {
                GlassSettingsItem(
                        icon =
                                if (currentThemeMode == ThemeMode.DARK) Icons.Filled.DarkMode
                                else Icons.Filled.LightMode,
                        title = "Theme",
                        subtitle =
                                when (currentThemeMode) {
                                    ThemeMode.LIGHT -> "Light Mode"
                                    ThemeMode.DARK -> "Dark Mode"
                                    ThemeMode.SYSTEM -> "System Default"
                                },
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val newMode =
                                    when (currentThemeMode) {
                                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                        ThemeMode.LIGHT -> ThemeMode.DARK
                                        ThemeMode.DARK -> ThemeMode.SYSTEM
                                    }
                            ThemeState.updateThemeMode(newMode)
                        },
                        trailing = {
                            Text(
                                    text =
                                            when (currentThemeMode) {
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                                ThemeMode.SYSTEM -> "System"
                                            },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                            )
                        }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Permissions", color = Primary)
            }

            item {
                GlassSettingsItem(
                        icon = Icons.Filled.Sms,
                        title = "SMS Permission",
                        subtitle =
                                if (smsPermissionGranted) "Granted"
                                else "Required to read bank transactions",
                        iconColor = Primary,
                        onClick = {
                            smsPermissionLauncher.launch(
                                    arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                    )
                            )
                        },
                        trailing = {
                            if (smsPermissionGranted) {
                                Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint = Success,
                                        modifier = Modifier.size(22.dp)
                                )
                            } else {
                                TextButton(
                                        onClick = {
                                            smsPermissionLauncher.launch(
                                                    arrayOf(
                                                            Manifest.permission.RECEIVE_SMS,
                                                            Manifest.permission.READ_SMS
                                                    )
                                            )
                                        }
                                ) { Text("Grant", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    GlassSettingsItem(
                            icon = Icons.Filled.Notifications,
                            title = "Notification Permission",
                            subtitle =
                                    if (notificationPermissionGranted) "Granted"
                                    else "Required for alerts",
                            iconColor = Primary,
                            onClick = {
                                notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                )
                            },
                            trailing = {
                                if (notificationPermissionGranted) {
                                    Icon(
                                            Icons.Filled.CheckCircle,
                                            null,
                                            tint = Success,
                                            modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    TextButton(
                                            onClick = {
                                                notificationPermissionLauncher.launch(
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            }
                                    ) { Text("Grant", fontWeight = FontWeight.SemiBold) }
                                }
                            }
                    )
                }
            }

            // Import Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Import", color = Accent)
            }

            item {
                val isImporting = smsImportState is ExpenseViewModel.SmsImportState.Importing
                GlassSettingsItem(
                        icon = Icons.Filled.ImportExport,
                        title = "Import Old SMS",
                        subtitle =
                                if (isImporting) "Importing..."
                                else "Scan SMS inbox for transactions",
                        iconColor = Accent,
                        onClick = {
                            if (!isImporting) viewModel.importHistoricalSms(daysBack = 365)
                        },
                        trailing = {
                            if (isImporting) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                )
                            }
                        }
                )
            }

            // Data Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Data", color = Info)
            }

            item {
                GlassSettingsItem(
                        icon = Icons.Filled.Category,
                        title = "Manage Categories",
                        subtitle = "Add, edit or remove categories",
                        iconColor = Info,
                        onClick = onNavigateToCategories
                )
            }

            item {
                GlassSettingsItem(
                        icon = Icons.Filled.FileDownload,
                        title = "Export Data",
                        subtitle = "Export expenses to CSV file",
                        iconColor = Info,
                        onClick = { /* TODO */}
                )
            }

            // Danger Zone
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Danger Zone", color = Secondary)
            }

            item {
                GlassSettingsItem(
                        icon = Icons.Filled.DeleteForever,
                        title = "Clear All Data",
                        subtitle = "Remove all expenses and mappings",
                        iconColor = Secondary,
                        onClick = { showClearDataDialog = true }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "About", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                GlassSettingsItem(
                        icon = Icons.Filled.Info,
                        title = "Expense Tracker",
                        subtitle = "Version 1.0",
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {}
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Import Result Dialog
    if (showImportDialog) {
        when (val state = smsImportState) {
            is ExpenseViewModel.SmsImportState.Success -> {
                AlertDialog(
                        onDismissRequest = {
                            showImportDialog = false
                            viewModel.resetImportState()
                        },
                        icon = { Icon(Icons.Filled.CheckCircle, null, tint = Primary) },
                        title = { Text("Import Complete!") },
                        text = {
                            Column {
                                Text("SMS scanned: ${state.totalSmsRead}")
                                Text("Bank SMS found: ${state.bankSmsFound}")
                                Text("Transactions imported: ${state.transactionsImported}")
                                Text("Duplicates skipped: ${state.duplicatesSkipped}")
                            }
                        },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        showImportDialog = false
                                        viewModel.resetImportState()
                                    }
                            ) { Text("Done") }
                        }
                )
            }
            is ExpenseViewModel.SmsImportState.Error -> {
                AlertDialog(
                        onDismissRequest = {
                            showImportDialog = false
                            viewModel.resetImportState()
                        },
                        icon = { Icon(Icons.Filled.Error, null, tint = Secondary) },
                        title = { Text("Import Failed") },
                        text = { Text(state.message) },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        showImportDialog = false
                                        viewModel.resetImportState()
                                    }
                            ) { Text("OK") }
                        }
                )
            }
            else -> {}
        }
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Data?") },
                text = {
                    Text(
                            "This will permanently delete all your expenses. This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = { showClearDataDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Secondary)
                    ) { Text("Delete All") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun GlassSettingsItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        iconColor: Color,
        onClick: () -> Unit,
        trailing: @Composable (() -> Unit)? = null
) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onClick)
    ) {
        // Subtle color tint
        Box(
                modifier =
                        Modifier.matchParentSize()
                                .background(
                                        brush =
                                                Brush.horizontalGradient(
                                                        colors =
                                                                listOf(
                                                                        iconColor.copy(
                                                                                alpha = 0.04f
                                                                        ),
                                                                        Color.Transparent
                                                                )
                                                )
                                )
        )

        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailing != null) {
                trailing()
            } else {
                Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
