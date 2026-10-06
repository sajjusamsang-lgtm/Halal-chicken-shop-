package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ErpTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel
import java.util.Locale

@Composable
fun ReportsAnalyticsScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("Month") } // Today, Week, Month, Custom
    val totalSales by viewModel.todayTotalSales.collectAsStateWithLifecycle()
    val netProfit by viewModel.todayNetProfit.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()

    // Period aggregations
    val periodRevenue = if (selectedPeriod == "Today") totalSales else 148500.0
    val periodCogs = if (selectedPeriod == "Today") totalSales * 0.65 else 94200.0
    val periodDirectExp = if (selectedPeriod == "Today") 1250.0 else 8500.0
    val periodOtherExp = if (selectedPeriod == "Today") 1800.0 else 12320.0
    val periodGrossProfit = periodRevenue - periodCogs
    val periodNetProfit = periodGrossProfit - periodDirectExp - periodOtherExp
    val marginPercentage = if (periodRevenue > 0) (periodNetProfit / periodRevenue) * 100 else 0.0

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Reports & Analytics",
                subtitle = "Profit & Loss • Monthly Audit",
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
            // Period Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today", "Week", "Month", "Custom").forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlight Cards: Total Sales & Net Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Sales", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            formatCurrency(periodRevenue),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldLight
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Net Profit", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            formatCurrency(periodNetProfit),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profit & Loss Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Profit & Loss ($selectedPeriod)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Revenue", style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(periodRevenue), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cost of Goods Sold (COGS)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(periodCogs), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Direct Expenses", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(periodDirectExp), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Other Overheads", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(periodOtherExp), style = MaterialTheme.typography.bodyMedium)
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Profit", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(formatCurrency(periodGrossProfit), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Profit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                        Text(formatCurrency(periodNetProfit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = GoldAccent)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Profit Margin", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.1f", marginPercentage)}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sales by Product Distribution
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sales by Product Category",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        Box(modifier = Modifier.weight(0.68f).fillMaxHeight().background(EmeraldLight))
                        Box(modifier = Modifier.weight(0.17f).fillMaxHeight().background(GoldAccent))
                        Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(Color(0xFF60A5FA)))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldLight))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chicken: 68%", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldAccent))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eggs: 17%", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Others: 15%", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Business Insights Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2B20)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Business Insights for Mr. Sajid", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                    }
                    Text("• Your average chicken selling price this month is ₹220/kg.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("• Net profit increased +18% compared to the previous week.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("• Institutional clients (DPS Biraul & Hotels) represent 45% of total volume.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("• Processing yield has maintained healthy ~80.5% with minimal wastage.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: HalalShopViewModel,
    onBackClick: () -> Unit
) {
    val pendingSales by viewModel.pendingSales.collectAsStateWithLifecycle()
    val totalPending = pendingSales.sumOf { it.balancePending }

    val notifications = remember(pendingSales) {
        val list = mutableListOf<Triple<String, String, ImageVector>>()
        list.add(
            Triple(
                "Institutional Schedule",
                "Delhi Public School Biraul (Sunday & Wednesday chicken supply schedule)",
                Icons.Default.LocalShipping
            )
        )
        if (totalPending > 0) {
            val topDebtor = pendingSales.groupBy { it.customerName }.maxByOrNull { entry -> entry.value.sumOf { it.balancePending } }
            if (topDebtor != null) {
                val debtorSum = topDebtor.value.sumOf { it.balancePending }
                list.add(
                    Triple(
                        "Pending Payment Notice",
                        "₹${String.format(Locale.US, "%.2f", debtorSum)} pending from ${topDebtor.key}",
                        Icons.Default.AccountBalanceWallet
                    )
                )
            }
            list.add(
                Triple(
                    "Total Outstanding",
                    "Total ₹${String.format(Locale.US, "%.2f", totalPending)} outstanding across ${pendingSales.size} bills",
                    Icons.Default.Warning
                )
            )
        } else {
            list.add(
                Triple(
                    "Accounts Cleared",
                    "No pending payments. All customer accounts are settled!",
                    Icons.Default.CheckCircle
                )
            )
        }
        list
    }

    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Notifications & Reminders",
                subtitle = "Alerts & Schedules",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications) { (title, subtitle, icon) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
