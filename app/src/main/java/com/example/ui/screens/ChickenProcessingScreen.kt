package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ErpTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChickenProcessingScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onProcessingSaved: () -> Unit
) {
    val context = LocalContext.current
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()

    var batchCodeInput by remember { mutableStateOf("") }
    var liveBirdCountInput by remember { mutableStateOf("") }
    var liveWeightInput by remember { mutableStateOf("") }
    var usableWeightInput by remember { mutableStateOf("") }
    var wasteWeightInput by remember { mutableStateOf("") }
    var otherLossInput by remember { mutableStateOf("0") }
    var totalBatchCostInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val liveWeight = liveWeightInput.toDoubleOrNull() ?: 0.0
    val usableWeight = usableWeightInput.toDoubleOrNull() ?: 0.0
    val wasteWeight = wasteWeightInput.toDoubleOrNull() ?: 0.0
    val otherLoss = otherLossInput.toDoubleOrNull() ?: 0.0
    val totalBatchCost = totalBatchCostInput.toDoubleOrNull() ?: 0.0

    // Real-time Yield calculations
    val yieldPercentage = if (liveWeight > 0) (usableWeight / liveWeight) * 100 else 0.0
    val totalLossWeight = wasteWeight + otherLoss
    val lossPercentage = if (liveWeight > 0) (totalLossWeight / liveWeight) * 100 else 0.0
    val costPerUsableKg = if (usableWeight > 0) totalBatchCost / usableWeight else 0.0

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Chicken Processing",
                subtitle = "Yield & Usable Cost Manager",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cost per Usable kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${String.format("%.2f", costPerUsableKg)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = GoldAccent
                        )
                    }

                    Button(
                        onClick = {
                            if (liveWeight <= 0 || usableWeight <= 0) {
                                Toast.makeText(context, "Please enter valid live & usable weights", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.saveProcessingRecord(
                                batchCode = batchCodeInput,
                                liveBirdCount = liveBirdCountInput.toIntOrNull() ?: 1,
                                liveWeightKg = liveWeight,
                                usableChickenWeightKg = usableWeight,
                                wasteOffalWeightKg = wasteWeight,
                                otherLossKg = otherLoss,
                                totalBatchCost = totalBatchCost,
                                notes = notesInput
                            ) {
                                Toast.makeText(context, "Processing Saved! Usable Cost: ₹${String.format("%.2f", costPerUsableKg)}/kg", Toast.LENGTH_LONG).show()
                                onProcessingSaved()
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_processing_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Processing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Select Batch from existing purchases
            if (purchases.isNotEmpty()) {
                Text(
                    text = "Select Live Batch",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(purchases) { p ->
                        FilterChip(
                            selected = batchCodeInput == p.batchCode,
                            onClick = {
                                batchCodeInput = p.batchCode
                                liveBirdCountInput = p.numberOfBirds.toString()
                                liveWeightInput = p.totalLiveWeightKg.toString()
                                totalBatchCostInput = p.totalCost.toInt().toString()
                                // Default estimated usable 80%
                                usableWeightInput = String.format("%.1f", p.totalLiveWeightKg * 0.80)
                                wasteWeightInput = String.format("%.1f", p.totalLiveWeightKg * 0.20)
                            },
                            label = { Text("${p.batchCode} (${p.totalLiveWeightKg.toInt()} kg)") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "No chicken stock recorded",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Log incoming bird batches first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo("chicken_purchase") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("+ Add Purchase", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = batchCodeInput,
                onValueChange = { batchCodeInput = it },
                label = { Text("Live Batch ID") },
                modifier = Modifier.fillMaxWidth().testTag("batch_id_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = liveWeightInput,
                    onValueChange = { liveWeightInput = it },
                    label = { Text("Live Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("processing_live_weight"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = usableWeightInput,
                    onValueChange = { usableWeightInput = it },
                    label = { Text("Final Usable (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("processing_usable_weight"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = wasteWeightInput,
                    onValueChange = { wasteWeightInput = it },
                    label = { Text("Waste / Offal (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = otherLossInput,
                    onValueChange = { otherLossInput = it },
                    label = { Text("Other Loss (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = totalBatchCostInput,
                onValueChange = { totalBatchCostInput = it },
                label = { Text("Total Batch Cost (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("total_batch_cost_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Calculated Yield & Loss Analytics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Yield %",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.2f", yieldPercentage)}%",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = EmeraldLight
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CrimsonContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Loss %",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.2f", lossPercentage)}%",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = CrimsonDanger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Educational note for ERP accuracy
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cost per usable kg (₹${String.format("%.2f", costPerUsableKg)}) will be automatically used to calculate the real Cost of Goods Sold (COGS) and accurate daily gross profit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
