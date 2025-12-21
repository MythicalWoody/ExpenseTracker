package com.example.expencetrackerapp.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expencetrackerapp.ui.theme.Primary
import com.example.expencetrackerapp.ui.theme.Secondary
import com.example.expencetrackerapp.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel, onNavigateToCategories: () -> Unit) {
    val context = LocalContext.current

    var smsPermissionGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

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

    LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Permissions Section
        item {
            Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                    icon = Icons.Filled.Sms,
                    title = "SMS Permission",
                    subtitle =
                            if (smsPermissionGranted) "Granted"
                            else "Required to read bank transactions",
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
                                    contentDescription = null,
                                    tint = Primary
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
                            ) { Text("Grant") }
                        }
                    }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                SettingsItem(
                        icon = Icons.Filled.Notifications,
                        title = "Notification Permission",
                        subtitle =
                                if (notificationPermissionGranted) "Granted"
                                else "Required for categorization alerts",
                        onClick = {
                            notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                            )
                        },
                        trailing = {
                            if (notificationPermissionGranted) {
                                Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Primary
                                )
                            } else {
                                TextButton(
                                        onClick = {
                                            notificationPermissionLauncher.launch(
                                                    Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                ) { Text("Grant") }
                            }
                        }
                )
            }
        }

        // SMS Import Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Import",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            val isImporting = smsImportState is ExpenseViewModel.SmsImportState.Importing
            SettingsItem(
                    icon = Icons.Filled.ImportExport,
                    title = "Import Old SMS",
                    subtitle =
                            if (isImporting) "Importing..."
                            else "Scan SMS inbox for bank transactions",
                    onClick = {
                        if (!isImporting) {
                            viewModel.importHistoricalSms(daysBack = 365)
                        }
                    },
                    trailing = {
                        if (isImporting) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = Primary)
                        }
                    }
            )
        }

        // Data Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                    icon = Icons.Filled.Category,
                    title = "Manage Categories",
                    subtitle = "Add, edit or remove categories",
                    onClick = onNavigateToCategories
            )
        }

        item {
            SettingsItem(
                    icon = Icons.Filled.FileDownload,
                    title = "Export Data",
                    subtitle = "Export expenses to CSV file",
                    onClick = {
                        // TODO: Implement export
                    }
            )
        }

        // Danger Zone
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleSmall,
                    color = Secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Clear All Data",
                    subtitle = "Remove all expenses and learned mappings",
                    onClick = { showClearDataDialog = true },
                    iconTint = Secondary
            )
        }

        // About Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Expense Tracker",
                    subtitle = "Version 1.0 • Made with ❤️",
                    onClick = {}
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
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
                                Text("📱 SMS scanned: ${state.totalSmsRead}")
                                Text("🏦 Bank SMS found: ${state.bankSmsFound}")
                                Text("✅ Transactions imported: ${state.transactionsImported}")
                                Text("⏭️ Duplicates skipped: ${state.duplicatesSkipped}")
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
                            "This will permanently delete all your expenses, categories, and learned merchant mappings. This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                // TODO: Implement clear data
                                showClearDataDialog = false
                            },
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
private fun SettingsItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        trailing: @Composable (() -> Unit)? = null,
        iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
