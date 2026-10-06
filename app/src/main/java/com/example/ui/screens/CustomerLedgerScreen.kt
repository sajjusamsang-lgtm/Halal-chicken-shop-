package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerType
import com.example.data.model.PaymentRecordEntity
import com.example.ui.components.ErpTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel

@Composable
fun CustomerLedgerScreen(
    customer: CustomerEntity,
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onPaymentSuccess: (PaymentRecordEntity) -> Unit
) {
    val context = LocalContext.current
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val allPayments by viewModel.payments.collectAsStateWithLifecycle()

    val customerSales = remember(allSales, customer) {
        allSales.filter { it.customerId == customer.id }
    }
    val customerPayments = remember(allPayments, customer) {
        allPayments.filter { it.customerId == customer.id }
    }

    val totalPurchases = customerSales.sumOf { it.finalAmount }
    val totalPaid = customerSales.sumOf { it.amountPaid } + customerPayments.sumOf { it.amountReceived }
    val totalPending = customerSales.sumOf { it.balancePending }

    var selectedTab by remember { mutableStateOf("Ledger") } // Ledger or Details

    Scaffold(
        topBar = {
            ErpTopBar(
                title = customer.name,
                subtitle = "Phone: ${customer.phone} | ${customer.customerType}",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.shareLedgerStatement(context, customer, customerSales, totalPending)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Statement", tint = EmeraldLight)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Row: Total Purchases, Total Paid, Total Pending
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Total Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatCurrency(totalPurchases), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Total Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatCurrency(totalPaid), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Total Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatCurrency(totalPending), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (totalPending > 0) AmberAccent else EmeraldLight)
                    }
                }
            }

            // Quick Customer Communication Actions Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Button
                    OutlinedButton(
                        onClick = { viewModel.callCustomer(context, customer.phone) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // WhatsApp Statement Button
                    Button(
                        onClick = {
                            if (totalPending > 0) {
                                val msg = "Dear ${customer.name},\nYour pending statement balance at HALAL CHICKEN SHOP HANTI is ₹${totalPending.toInt()}.\nPlease clear at your convenience.\nOwner: Mr. Sajid (+91 9708099035)"
                                viewModel.chatCustomerWhatsApp(context, customer.phone, msg)
                            } else {
                                viewModel.downloadOrSharePendingStatementPdf(context, customer, isPrint = false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // SMS Reminder Button
                    OutlinedButton(
                        onClick = {
                            val reminder = "Dear ${customer.name}, your Khata pending balance at HALAL CHICKEN SHOP HANTI is ₹${totalPending.toInt()}. Please settle soon. Mr. Sajid (+91 9708099035)."
                            viewModel.sendCustomerSms(context, customer.phone, reminder)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS", tint = AmberAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // PDF Statement Button
                    OutlinedButton(
                        onClick = {
                            viewModel.downloadOrSharePendingStatementPdf(context, customer, isPrint = false)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "PDF", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Statement", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Tabs (Ledger vs Details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("Ledger", "Details").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == "Ledger") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customerSales.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No bills generated",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No purchase or delivery bills recorded for this customer yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { viewModel.navigateTo("quick_sale") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate Bill")
                                    }
                                }
                            }
                        }
                    }

                    items(customerSales) { sale ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Bill #${sale.billNumber}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${sale.saleDate} • ${sale.quantity} ${sale.quantityUnit} ${sale.chickenCut} @ ₹${sale.sellingRate.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(sale.finalAmount),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        StatusBadge(status = sale.paymentStatus)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.downloadOrShareInvoicePdf(context, sale, isPrint = true)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Print", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.downloadOrShareInvoicePdf(context, sale, isPrint = false)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDF", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.shareBillWhatsApp(context, sale)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Profile & Details tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Customer Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Business Name: ${customer.businessName.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Contact Phone: ${customer.phone}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Billing Cycle: ${customer.paymentCycle}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Default Rate: ₹${customer.defaultChickenRate.toInt()}/kg", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Scheduled Deliveries: ${customer.deliveryDays.ifBlank { "None configured" }}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Address: ${customer.address.ifBlank { "Hanti" }}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCustomersScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("Schools") } // Schools, Hotels, Restaurants, Others
    var showDeliveryDialogForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deliveryKgInput by remember { mutableStateOf("40.0") }

    val filteredCustomers = customers.filter { cust ->
        when (selectedTab) {
            "Schools" -> cust.customerType == CustomerType.SCHOOL.name
            "Hotels" -> cust.customerType == CustomerType.HOTEL.name
            "Restaurants" -> cust.customerType == CustomerType.RESTAURANT.name
            else -> cust.customerType != CustomerType.SCHOOL.name && cust.customerType != CustomerType.HOTEL.name && cust.customerType != CustomerType.RESTAURANT.name
        }
    }

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Business Customers",
                subtitle = "Institutional Supply & Schedules",
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
        ) {
            // Category Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Schools", "Hotels", "Restaurants", "Others").forEach { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldLight
                        )
                    )
                }
            }

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers) { cust ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectCustomer(cust) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (cust.customerType) {
                                                CustomerType.SCHOOL.name -> Icons.Default.School
                                                CustomerType.HOTEL.name -> Icons.Default.Hotel
                                                CustomerType.RESTAURANT.name -> Icons.Default.Restaurant
                                                else -> Icons.Default.Business
                                            },
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = cust.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Contact: ${cust.phone} • Payment: ${cust.paymentCycle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldLight
                                    )
                                }
                            }

                            if (cust.deliveryDays.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Supply: ${cust.deliveryDays}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = AmberAccent
                                    )

                                    Button(
                                        onClick = {
                                            showDeliveryDialogForCustomer = cust
                                            deliveryKgInput = if (cust.name.contains("DPS")) "40.0" else "25.0"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log Delivery", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to log scheduled delivery for school/hotel
    if (showDeliveryDialogForCustomer != null) {
        val cust = showDeliveryDialogForCustomer!!
        AlertDialog(
            onDismissRequest = { showDeliveryDialogForCustomer = null },
            title = { Text("Log Delivery Bill") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Customer: ${cust.name}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Default Rate: ₹${cust.defaultChickenRate.toInt()}/kg", style = MaterialTheme.typography.bodySmall, color = GoldAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deliveryKgInput,
                        onValueChange = { deliveryKgInput = it },
                        label = { Text("Delivered Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = deliveryKgInput.toDoubleOrNull() ?: 0.0
                        viewModel.saveSale(
                            customerId = cust.id,
                            customerName = cust.name,
                            customerType = cust.customerType,
                            productType = "Chicken",
                            chickenCut = "Curry Cut",
                            quantityUnit = "KG",
                            quantity = kg,
                            piecesCount = 20,
                            sellingRate = cust.defaultChickenRate,
                            discount = 0.0,
                            paymentMethod = "Pending",
                            amountPaidManual = 0.0,
                            dueDate = "",
                            isInstitutionalDelivery = true,
                            notes = "Scheduled ${cust.deliveryDays} delivery"
                        ) {
                            Toast.makeText(context, "Delivery Logged: Bill ${it.billNumber}", Toast.LENGTH_LONG).show()
                            showDeliveryDialogForCustomer = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Generate Daily Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeliveryDialogForCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
