package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.*
import com.example.ui.components.ErpTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onSaleSaved: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("Chicken") } // Chicken or Eggs
    var selectedCut by remember { mutableStateOf(ChickenCut.CURRY_CUT.label) }
    var selectedUnit by remember { mutableStateOf(WeightUnit.KG.label) }

    val currentChickenRate = settings?.defaultChickenRate ?: 300.0
    var quantityInput by remember { mutableStateOf("1.000") }
    var rateInput by remember(settings) { mutableStateOf(currentChickenRate.toInt().toString()) }
    var discountInput by remember { mutableStateOf("0") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customCustomerName by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") } // Cash, UPI, Pending, Partial
    var partialAmountPaidInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var savedSaleDialog by remember { mutableStateOf<SaleEntity?>(null) }

    // Auto-update rate when customer is selected (e.g. DPS special rate) or when tab toggles
    LaunchedEffect(selectedCustomer, selectedTab, settings) {
        if (selectedTab == "Chicken") {
            if (selectedCustomer != null && selectedCustomer!!.defaultChickenRate > 0) {
                rateInput = selectedCustomer!!.defaultChickenRate.toInt().toString()
            } else {
                rateInput = (settings?.defaultChickenRate ?: 300.0).toInt().toString()
            }
        } else {
            rateInput = (settings?.defaultEggTrayRateBrown ?: 180.0).toInt().toString()
        }
    }

    // Instant calculations: Total = Quantity * Rate
    val quantityValue = quantityInput.toDoubleOrNull() ?: 0.0
    val rateValue = rateInput.toDoubleOrNull() ?: 0.0
    val discountValue = discountInput.toDoubleOrNull() ?: 0.0

    val subtotal = when (selectedUnit) {
        "Gram" -> (quantityValue / 1000.0) * rateValue
        else -> quantityValue * rateValue
    }
    val finalAmount = (subtotal - discountValue).coerceAtLeast(0.0)

    val chickenCuts = listOf(
        ChickenCut.WHOLE.label,
        ChickenCut.CURRY_CUT.label,
        ChickenCut.BONELESS.label,
        ChickenCut.LEG_PIECE.label,
        ChickenCut.BREAST.label,
        ChickenCut.SMALL_PIECES.label
    )

    val baseRate = (settings?.defaultChickenRate ?: 300.0).toInt()
    val quickRates = listOf(baseRate - 20, baseRate - 10, baseRate, baseRate + 10, baseRate + 20)

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Quick Sale",
                subtitle = "Fast Billing & POS",
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
                            text = "Total Payable",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(finalAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = GoldAccent
                        )
                    }

                    Button(
                        onClick = {
                            if (quantityValue <= 0) {
                                Toast.makeText(context, "Please enter valid quantity", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val customerName = if (selectedCustomer != null) {
                                selectedCustomer!!.name
                            } else if (customCustomerName.isNotBlank()) {
                                customCustomerName
                            } else {
                                "Walk-in Retail"
                            }

                            val customerType = selectedCustomer?.customerType ?: CustomerType.RETAIL.name

                            val partialPaid = if (selectedPaymentMethod == "Partial") {
                                partialAmountPaidInput.toDoubleOrNull() ?: 0.0
                            } else null

                            val productType = if (selectedTab == "Chicken") ProductType.CHICKEN.name else ProductType.EGG.name

                            viewModel.saveSale(
                                customerId = selectedCustomer?.id,
                                customerName = customerName,
                                customerType = customerType,
                                productType = productType,
                                chickenCut = if (selectedTab == "Chicken") selectedCut else "Table Eggs",
                                quantityUnit = selectedUnit,
                                quantity = quantityValue,
                                piecesCount = if (selectedUnit == "Piece") quantityValue.toInt() else 1,
                                sellingRate = rateValue,
                                discount = discountValue,
                                paymentMethod = selectedPaymentMethod,
                                amountPaidManual = partialPaid,
                                dueDate = "",
                                isInstitutionalDelivery = selectedCustomer?.customerType == CustomerType.SCHOOL.name || selectedCustomer?.customerType == CustomerType.HOTEL.name,
                                notes = notesInput
                            ) { createdSale ->
                                Toast.makeText(context, "Sale recorded: ${createdSale.billNumber}", Toast.LENGTH_SHORT).show()
                                savedSaleDialog = createdSale
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_sale_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Sale",
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
            // Tab Selector (Chicken vs Eggs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("Chicken", "Eggs").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                            .clickable {
                                selectedTab = tab
                                if (tab == "Eggs") {
                                    selectedUnit = "Tray"
                                    rateInput = "180"
                                } else {
                                    selectedUnit = "KG"
                                    rateInput = "220"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == "Chicken") {
                // Cut Selector
                Text(
                    text = "Select Cut / Preparation",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chickenCuts) { cut ->
                        val isSelected = selectedCut == cut
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCut = cut },
                            label = { Text(cut) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldLight
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Unit Selector
            Text(
                text = "Weight / Quantity Unit",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val units = if (selectedTab == "Chicken") listOf("KG", "Gram", "Piece") else listOf("Tray", "Cardboard", "Piece")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                units.forEach { unit ->
                    val isSelected = selectedUnit == unit
                    OutlinedButton(
                        onClick = { selectedUnit = unit },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) EmeraldContainer else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldLight else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity & Rate Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Quantity / $selectedUnit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("quantity_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Rate (₹ / $selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("rate_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Rate Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                quickRates.forEach { rate ->
                    SuggestionChip(
                        onClick = { rateInput = rate.toString() },
                        label = { Text("₹$rate", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Discount Field
            OutlinedTextField(
                value = discountInput,
                onValueChange = { discountInput = it },
                label = { Text("Discount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cash", "UPI", "Pending", "Partial").forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    val bgCol = when (method) {
                        "Cash" -> Color(0xFF0F5132)
                        "UPI" -> Color(0xFF1D4ED8)
                        "Pending" -> Color(0xFFB91C1C)
                        else -> Color(0xFFB45309)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) bgCol else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedPaymentMethod = method }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedPaymentMethod == "Partial") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = partialAmountPaidInput,
                    onValueChange = { partialAmountPaidInput = it },
                    label = { Text("Amount Paid Now (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Selection
            Text(
                text = "Customer (Optional for Retail)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCustomer == null,
                        onClick = { selectedCustomer = null },
                        label = { Text("Walk-in Retail") }
                    )
                }
                items(customers) { cust ->
                    FilterChip(
                        selected = selectedCustomer?.id == cust.id,
                        onClick = {
                            selectedCustomer = cust
                            rateInput = cust.defaultChickenRate.toInt().toString()
                        },
                        label = { Text(cust.name) }
                    )
                }
            }

            if (selectedCustomer == null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customCustomerName,
                    onValueChange = { customCustomerName = it },
                    label = { Text("Custom Customer Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Notes / Bill Details (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (savedSaleDialog != null) {
        val sale = savedSaleDialog!!
        AlertDialog(
            onDismissRequest = {
                savedSaleDialog = null
                onSaleSaved()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sale Recorded: ${sale.billNumber}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Customer: ${sale.customerName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Item: ${sale.quantity}${sale.quantityUnit} @ ₹${sale.sellingRate.toInt()}/${sale.quantityUnit}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Grand Total: ₹${sale.finalAmount.toInt()} (${sale.paymentStatus})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                    if (sale.balancePending > 0) {
                        Text(text = "Balance Pending: ₹${sale.balancePending.toInt()}", style = MaterialTheme.typography.bodySmall, color = AmberAccent)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Bill Options:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.shareInvoiceWhatsApp(context, sale)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                viewModel.downloadOrShareInvoicePdf(context, sale, isPrint = false)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                viewModel.downloadOrShareInvoicePdf(context, sale, isPrint = true)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        savedSaleDialog = null
                        onSaleSaved()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
