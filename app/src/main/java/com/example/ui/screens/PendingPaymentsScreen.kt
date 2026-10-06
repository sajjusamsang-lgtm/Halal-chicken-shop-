package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecordEntity
import com.example.data.model.SaleEntity
import com.example.ui.components.ErpTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PendingPaymentsScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onPaymentSuccess: (PaymentRecordEntity) -> Unit
) {
    val context = LocalContext.current
    val pendingSales by viewModel.pendingSales.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") } // All, Pending, Partial, Overdue
    var paymentDialogCustomer by remember { mutableStateOf<Pair<Long, String>?>(null) } // customerId to name
    var paymentDialogBalance by remember { mutableStateOf(0.0) }

    // Group sales by customer to aggregate total pending per customer
    val customerPendingMap = remember(pendingSales) {
        pendingSales.groupBy { it.customerId ?: -1L }.mapValues { entry ->
            val custSales = entry.value
            val totalPending = custSales.sumOf { it.balancePending }
            val custName = custSales.firstOrNull()?.customerName ?: "Retail"
            val earliestDue = custSales.minByOrNull { it.timestamp }?.dueDate ?: "Due Soon"
            val isPartial = custSales.any { it.paymentStatus == "PARTIAL" }
            val isOverdue = custSales.any { it.paymentStatus == "OVERDUE" || it.dueDate.contains("2026-04-20") }
            val status = if (isOverdue) "Overdue" else if (isPartial) "Partial" else "Pending"
            Triple(custName, totalPending, Pair(earliestDue, status))
        }
    }

    val totalReceivables = pendingSales.sumOf { it.balancePending }

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Pending Payments",
                subtitle = "Khata & Collections",
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
            // Total Outstanding Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Pending Khata",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalReceivables),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = GoldAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Partial", "Overdue").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pending Customers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customerPendingMap.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No pending payments",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldLight
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "All customer accounts and bills are fully settled.",
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

                items(customerPendingMap.entries.toList()) { (custId, data) ->
                    val (custName, amount, statusPair) = data
                    val (dueDate, status) = statusPair
                    val custObj = customers.firstOrNull { it.id == custId }
                    val custPhone = custObj?.phone ?: ""

                    val matchesFilter = when (selectedFilter) {
                        "Pending" -> status == "Pending"
                        "Partial" -> status == "Partial"
                        "Overdue" -> status == "Overdue"
                        else -> true
                    }

                    if (matchesFilter && amount > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (custId > 0) onCustomerClick(custId)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = custName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldLight
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = custName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Due: $dueDate • $custPhone",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(amount),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = if (status == "Overdue") CrimsonDanger else GoldAccent
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        StatusBadge(status = status.uppercase())
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (custPhone.isNotBlank()) {
                                            IconButton(
                                                onClick = { viewModel.callCustomer(context, custPhone) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = "Call",
                                                    tint = EmeraldLight,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val msg = "Dear $custName,\nYour pending Khata balance at HALAL CHICKEN SHOP HANTI is ₹${amount.toInt()}.\nPlease clear at your convenience.\nOwner: Mr. Sajid (+91 9708099035)"
                                                    viewModel.chatCustomerWhatsApp(context, custPhone, msg)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "WhatsApp",
                                                    tint = Color(0xFF25D366),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val reminder = "Dear $custName, your pending balance at HALAL CHICKEN SHOP HANTI is ₹${amount.toInt()}. Settle via UPI/Cash to Mr. Sajid (+91 9708099035)."
                                                    viewModel.sendCustomerSms(context, custPhone, reminder)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sms,
                                                    contentDescription = "SMS",
                                                    tint = AmberAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (custObj != null) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.downloadOrSharePendingStatementPdf(context, custObj, isPrint = false)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Statement PDF",
                                                    tint = Color(0xFF60A5FA),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            paymentDialogCustomer = Pair(custId, custName)
                                            paymentDialogBalance = amount
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Collect",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Collection Modal Dialog
    if (paymentDialogCustomer != null) {
        val custId = paymentDialogCustomer!!.first
        val custName = paymentDialogCustomer!!.second
        var paymentAmountInput by remember { mutableStateOf(paymentDialogBalance.toInt().toString()) }
        var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI.name) }
        var referenceIdInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("Bill settlement") }

        AlertDialog(
            onDismissRequest = { paymentDialogCustomer = null },
            title = {
                Text(
                    text = "Receive Payment",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customer: $custName",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Total Balance Due: ${formatCurrency(paymentDialogBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Amount Received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("receive_payment_amount_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Payment Mode", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(PaymentMethod.CASH.name, PaymentMethod.UPI.name, PaymentMethod.BANK_TRANSFER.name).forEach { m ->
                            FilterChip(
                                selected = selectedMethod == m,
                                onClick = { selectedMethod = m },
                                label = { Text(m, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = referenceIdInput,
                        onValueChange = { referenceIdInput = it },
                        label = { Text("Ref / UPI Txn ID (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) {
                            Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.receivePayment(
                            customerId = custId,
                            amountReceived = amount,
                            paymentMethod = selectedMethod,
                            referenceId = referenceIdInput,
                            notes = notesInput
                        ) { paymentRecord ->
                            paymentDialogCustomer = null
                            onPaymentSuccess(paymentRecord)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Confirm Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentDialogCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PaymentSuccessScreen(
    paymentRecord: PaymentRecordEntity,
    onViewLedger: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                // Green Success Crest
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                        .border(3.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = GoldAccent,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PAYMENT SUCCESSFUL",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = EmeraldLight
                )

                Text(
                    text = "HALAL CHICKEN SHOP HANTI",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Payment Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Customer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(paymentRecord.customerName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Paid", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(paymentRecord.amountReceived), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = EmeraldLight)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(paymentRecord.previousBalance), style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatCurrency(paymentRecord.remainingBalance),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (paymentRecord.remainingBalance <= 0) EmeraldLight else AmberAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(paymentRecord.paymentMethod, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date & Time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(paymentRecord.paymentDate, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewLedger,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Ledger")
                    }

                    OutlinedButton(
                        onClick = {
                            val msg = "Payment of ${formatCurrency(paymentRecord.amountReceived)} received from ${paymentRecord.customerName}. Remaining Balance: ${formatCurrency(paymentRecord.remainingBalance)}. Thank you! HALAL CHICKEN SHOP HANTI (Mr. Sajid)"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Payment Receipt"))
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Receipt")
                    }
                }
            }
        }
    }
}
