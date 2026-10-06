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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.components.ErpTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChickenPurchaseScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onPurchaseSaved: () -> Unit
) {
    val context = LocalContext.current
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var numberOfBirdsInput by remember { mutableStateOf("") }
    var totalLiveWeightInput by remember { mutableStateOf("") }
    var purchaseRateInput by remember { mutableStateOf("") }
    var transportCostInput by remember { mutableStateOf("0") }
    var otherCostInput by remember { mutableStateOf("0") }
    var selectedPaymentStatus by remember { mutableStateOf(PaymentStatus.PAID.name) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH.name) }
    var notesInput by remember { mutableStateOf("") }

    val numberOfBirds = numberOfBirdsInput.toIntOrNull() ?: 0
    val totalLiveWeight = totalLiveWeightInput.toDoubleOrNull() ?: 0.0
    val purchaseRate = purchaseRateInput.toDoubleOrNull() ?: 0.0
    val transportCost = transportCostInput.toDoubleOrNull() ?: 0.0
    val otherCost = otherCostInput.toDoubleOrNull() ?: 0.0

    // Auto-calculated fields
    val avgBirdWeight = if (numberOfBirds > 0) totalLiveWeight / numberOfBirds else 0.0
    val purchaseAmount = totalLiveWeight * purchaseRate
    val totalAcquisitionCost = purchaseAmount + transportCost + otherCost

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Live Chicken Purchase",
                subtitle = "Batch Stock Inward",
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
                            text = "Total Acquisition Cost",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalAcquisitionCost),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = GoldAccent
                        )
                    }

                    Button(
                        onClick = {
                            if (totalLiveWeight <= 0 || numberOfBirds <= 0) {
                                Toast.makeText(context, "Please enter valid live birds and weight", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.saveLiveChickenPurchase(
                                supplierName = supplierName,
                                supplierPhone = supplierPhone,
                                numberOfBirds = numberOfBirds,
                                totalLiveWeightKg = totalLiveWeight,
                                purchaseRatePerKg = purchaseRate,
                                transportCost = transportCost,
                                otherCost = otherCost,
                                paymentStatus = selectedPaymentStatus,
                                paymentMethod = selectedPaymentMethod,
                                notes = notesInput
                            ) { created ->
                                Toast.makeText(context, "Purchase Recorded! Batch ${created.batchCode}", Toast.LENGTH_SHORT).show()
                                onPurchaseSaved()
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_purchase_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Purchase",
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
            // Supplier Details
            Text(
                text = "Supplier Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("Supplier Name") },
                modifier = Modifier.fillMaxWidth().testTag("supplier_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = supplierPhone,
                onValueChange = { supplierPhone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Birds & Weight
            Text(
                text = "Bird Count & Live Weight",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = numberOfBirdsInput,
                    onValueChange = { numberOfBirdsInput = it },
                    label = { Text("Number of Birds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("number_of_birds_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalLiveWeightInput,
                    onValueChange = { totalLiveWeightInput = it },
                    label = { Text("Total Live Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("live_weight_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated Average Weight / Bird
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avg. Weight / Bird",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.2f", avgBirdWeight)} kg",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Rate & Costs
            Text(
                text = "Pricing & Transportation",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = purchaseRateInput,
                onValueChange = { purchaseRateInput = it },
                label = { Text("Purchase Rate (₹/kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = transportCostInput,
                    onValueChange = { transportCostInput = it },
                    label = { Text("Transport Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = otherCostInput,
                    onValueChange = { otherCostInput = it },
                    label = { Text("Other Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Status & Method
            Text(
                text = "Payment Status",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentStatus.PAID.name, PaymentStatus.PARTIAL.name, PaymentStatus.PENDING.name).forEach { st ->
                    FilterChip(
                        selected = selectedPaymentStatus == st,
                        onClick = { selectedPaymentStatus = st },
                        label = { Text(st) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentMethod.CASH.name, PaymentMethod.UPI.name, PaymentMethod.BANK_TRANSFER.name).forEach { m ->
                    FilterChip(
                        selected = selectedPaymentMethod == m,
                        onClick = { selectedPaymentMethod = m },
                        label = { Text(m) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
