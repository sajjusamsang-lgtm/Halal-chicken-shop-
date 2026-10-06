package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ErpTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@Composable
fun SettingsScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val integrityReport by viewModel.dataIntegrityReport.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showIntegrityDialog by remember { mutableStateOf(false) }
    var isRunningIntegrityCheck by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Settings",
                subtitle = "Owner Preferences & System",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsCategoryHeader("Business Information")

            SettingsRowItem(
                icon = Icons.Default.Storefront,
                title = "Shop Information",
                subtitle = "HALAL CHICKEN SHOP HANTI • Mr. Sajid",
                onClick = {
                    Toast.makeText(context, "HALAL CHICKEN SHOP HANTI (Hanti, Bihar)", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsRowItem(
                icon = Icons.Default.Receipt,
                title = "Invoice & Rates Settings",
                subtitle = "Prefix: HCSH • Default Chicken Rate: ₹220/kg",
                onClick = {
                    Toast.makeText(context, "Default Chicken Rate: ₹220/kg, Egg Tray: ₹180", Toast.LENGTH_SHORT).show()
                }
            )

            SettingsCategoryHeader("Security & Access")

            SettingsRowItem(
                icon = Icons.Default.Lock,
                title = "PIN / Security",
                subtitle = "Change owner login PIN",
                onClick = { showPinDialog = true }
            )

            SettingsRowItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Authentication",
                subtitle = if (settings?.biometricEnabled == true) "Enabled" else "Disabled",
                trailing = {
                    Switch(
                        checked = settings?.biometricEnabled ?: true,
                        onCheckedChange = { viewModel.toggleBiometric() }
                    )
                }
            )

            SettingsCategoryHeader("System & Data")

            SettingsRowItem(
                icon = Icons.Default.DarkMode,
                title = "Appearance",
                subtitle = if (settings?.isDarkMode == true) "Dark Mode" else "Light Mode",
                trailing = {
                    Switch(
                        checked = settings?.isDarkMode ?: true,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            )

            SettingsRowItem(
                icon = Icons.Default.CloudSync,
                title = "Cloud Synchronization",
                subtitle = "Status: $syncStatus",
                trailing = {
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (syncStatus.contains("✓")) EmeraldLight else GoldAccent
                    )
                }
            )

            SettingsRowItem(
                icon = Icons.Default.Backup,
                title = "Export Business Data (JSON)",
                subtitle = "Complete backup of all collections & ledger",
                onClick = {
                    viewModel.exportBackupJson(context)
                }
            )

            SettingsRowItem(
                icon = Icons.Default.TableChart,
                title = "Export Sales Ledger (CSV)",
                subtitle = "Spreadsheet export for accounting",
                onClick = {
                    viewModel.exportBackupCsv(context)
                }
            )

            SettingsRowItem(
                icon = Icons.Default.Restore,
                title = "Safe Database Restore",
                subtitle = "Validate and merge backup records",
                onClick = { showRestoreDialog = true }
            )

            SettingsRowItem(
                icon = Icons.Default.VerifiedUser,
                title = "Run Data Integrity Check",
                subtitle = "Scan for negative stock, orphan records & duplicate bills",
                onClick = {
                    isRunningIntegrityCheck = true
                    coroutineScope.launch {
                        viewModel.runDataIntegrityCheck()
                        isRunningIntegrityCheck = false
                        showIntegrityDialog = true
                    }
                }
            )

            SettingsCategoryHeader("About")

            SettingsRowItem(
                icon = Icons.Default.Info,
                title = "About HALAL CHICKEN SHOP HANTI",
                subtitle = "Designed & Developed by Mr. Sajid",
                onClick = onNavigateAbout
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout from Owner Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HALAL CHICKEN SHOP HANTI",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldLight
                )
                Text(
                    text = "Designed & Developed by Mr. Sajid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Change Owner PIN") },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 6) newPinInput = it },
                    label = { Text("Enter New PIN (4-6 digits)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length >= 4) {
                            viewModel.updatePin(newPinInput)
                            showPinDialog = false
                            Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showIntegrityDialog && integrityReport != null) {
        val rep = integrityReport!!
        AlertDialog(
            onDismissRequest = { showIntegrityDialog = false },
            icon = {
                Icon(
                    imageVector = if (rep.overallStatus == "PASS") Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (rep.overallStatus == "PASS") EmeraldLight else CrimsonDanger
                )
            },
            title = { Text("Data Integrity: ${rep.overallStatus}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Records Scanned: ${rep.totalChecked}")
                    Text("Errors: ${rep.errorCount} | Warnings: ${rep.warningCount}")
                    if (rep.issues.isEmpty()) {
                        Text("All business records, yields, stock traces, and invoice balances are clean and consistent.", color = EmeraldLight)
                    } else {
                        for (issue in rep.issues.take(5)) {
                            Text("• [${issue.code}] ${issue.message}", style = MaterialTheme.typography.bodySmall, color = if (issue.severity == "ERROR") CrimsonDanger else GoldAccent)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showIntegrityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Safe Database Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your JSON backup data below. The system validates shop ownership and schema safety before merging records into your active shop database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = restoreInputJson,
                        onValueChange = { restoreInputJson = it },
                        label = { Text("Paste JSON Backup") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInputJson.isNotBlank()) {
                            viewModel.restoreBackupJson(restoreInputJson) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    showRestoreDialog = false
                                    restoreInputJson = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Validate & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            ErpTopBar(
                title = "About",
                subtitle = "App Details & Developer Credit",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                        .border(3.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_brand_logo),
                        contentDescription = "Halal Chicken Shop Hanti Logo",
                        modifier = Modifier
                            .size(94.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "HALAL CHICKEN SHOP HANTI",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Owner: Mr. Sajid",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldLight)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Shop Address: Hanti, Bihar, India", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = EmeraldLight)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Contact: +91 9876543210", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = EmeraldLight)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Private Owner ERP Edition", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Designed & Developed by Mr. Sajid",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Version 1.0.0 • Together for a Better Business",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = EmeraldLight,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}
