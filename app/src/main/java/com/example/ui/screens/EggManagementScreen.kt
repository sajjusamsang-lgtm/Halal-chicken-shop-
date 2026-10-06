package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EggType
import com.example.data.model.EggUnit
import com.example.data.model.PaymentMethod
import com.example.ui.components.ErpTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EggManagementScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val eggTransactions by viewModel.eggTransactions.collectAsStateWithLifecycle()

    var selectedEggType by remember { mutableStateOf(EggType.BROWN.name) } // BROWN vs WHITE
    var selectedUnit by remember { mutableStateOf(EggUnit.TRAY.name) } // TRAY, CARDBOARD, PIECE
    var quantityInput by remember { mutableStateOf("2") }
    var rateInput by remember { mutableStateOf("180") }
    var customerNameInput by remember { mutableStateOf("Retail Customer") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH.name) }

    val quantityValue = quantityInput.toDoubleOrNull() ?: 0.0
    val rateValue = rateInput.toDoubleOrNull() ?: 0.0
    val totalAmount = quantityValue * rateValue

    val unitEggCount = when (selectedUnit) {
        EggUnit.TRAY.name -> 30
        EggUnit.CARDBOARD.name -> 210
        else -> 1
    }
    val totalCalculatedEggs = (quantityValue * unitEggCount).toInt()

    // Aggregate statistics
    val brownSales = eggTransactions.filter { it.eggType == EggType.BROWN.name && it.transactionType == "SALE" }
    val whiteSales = eggTransactions.filter { it.eggType == EggType.WHITE.name && it.transactionType == "SALE" }
    val currentSales = if (selectedEggType == EggType.BROWN.name) brownSales else whiteSales

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Egg Management",
                subtitle = "Stock, Trays & Sales",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Egg Type Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(EggType.BROWN.name to "Brown Eggs", EggType.WHITE.name to "White Eggs").forEach { (typeKey, label) ->
                    val isSelected = selectedEggType == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                            .clickable {
                                selectedEggType = typeKey
                                rateInput = if (typeKey == EggType.BROWN.name) "180" else "170"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Conversion Information Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Standard Packaging Conversion:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "1 Tray = 30 Eggs • 1 Cardboard = 210 Eggs (7 Trays)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stock Overview
            Text(
                text = "Stock Overview",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val typePurchases = eggTransactions.filter { it.eggType == selectedEggType && it.transactionType == "PURCHASE" }
            val totalPurchasedTrays = typePurchases.sumOf { it.unitQuantity }
            val typeSales = eggTransactions.filter { it.eggType == selectedEggType && it.transactionType == "SALE" }
            val totalSoldEggs = typeSales.sumOf { it.totalEggsCount }
            val totalSoldTrays = totalSoldEggs / 30.0
            val remainingTrays = (totalPurchasedTrays - totalSoldTrays).coerceAtLeast(0.0)

            if (typePurchases.isEmpty() && typeSales.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = "No egg stock recorded",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Record incoming egg trays or record sale below",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Purchased", style = MaterialTheme.typography.labelSmall)
                            Text("${totalPurchasedTrays.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text("trays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sold", style = MaterialTheme.typography.labelSmall)
                            Text("$totalSoldEggs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                            Text("eggs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Remaining", style = MaterialTheme.typography.labelSmall)
                            Text("${remainingTrays.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
                            Text("trays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sales Entry Form
            Text(
                text = "Fast Egg Sales Entry",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Unit Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(EggUnit.TRAY.name, EggUnit.CARDBOARD.name, EggUnit.PIECE.name).forEach { u ->
                    val isSelected = selectedUnit == u
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedUnit = u
                            rateInput = when (u) {
                                EggUnit.TRAY.name -> if (selectedEggType == EggType.BROWN.name) "180" else "170"
                                EggUnit.CARDBOARD.name -> if (selectedEggType == EggType.BROWN.name) "1200" else "1150"
                                else -> "7"
                            }
                        },
                        label = { Text(u) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantity ($selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Rate (₹ / $selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated Eggs Count & Amount
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: $totalCalculatedEggs eggs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatCurrency(totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = GoldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Mode
            Text(text = "Payment Mode", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentMethod.CASH.name, PaymentMethod.UPI.name).forEach { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (quantityValue <= 0) {
                        Toast.makeText(context, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.addEggSale(
                        eggType = selectedEggType,
                        unit = selectedUnit,
                        quantity = quantityValue,
                        rate = rateValue,
                        paymentMethod = selectedPaymentMethod,
                        customerName = customerNameInput
                    ) {
                        Toast.makeText(context, "Egg Sale Saved! ₹${totalAmount.toInt()}", Toast.LENGTH_SHORT).show()
                        quantityInput = "1"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_egg_sale_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Egg Sale",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
