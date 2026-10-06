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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HalalShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: HalalShopViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val totalSales by viewModel.todayTotalSales.collectAsStateWithLifecycle()
    val netProfit by viewModel.todayNetProfit.collectAsStateWithLifecycle()
    val cashReceived by viewModel.todayCashReceived.collectAsStateWithLifecycle()
    val upiReceived by viewModel.todayUpiReceived.collectAsStateWithLifecycle()
    val pendingToday by viewModel.todayPendingSales.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.todayTotalExpenses.collectAsStateWithLifecycle()
    val totalOutstanding by viewModel.totalOutstandingReceivables.collectAsStateWithLifecycle()

    val liveChickenStats by viewModel.liveChickenPurchasedToday.collectAsStateWithLifecycle()
    val chickenSoldStats by viewModel.chickenSoldToday.collectAsStateWithLifecycle()
    val eggsSoldStats by viewModel.eggsSoldToday.collectAsStateWithLifecycle()

    val recentSales by viewModel.selectedDateSales.collectAsStateWithLifecycle()
    val pendingList by viewModel.pendingSales.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showPriceDialog by remember { mutableStateOf(false) }
    var newRateInput by remember { mutableStateOf("") }

    val formattedToday = remember {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val weeklyData = remember(allSales) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val list = mutableListOf<Triple<String, Double, Float>>()
        val dayTotals = mutableListOf<Double>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = sdf.format(c.time)
            val dayLabel = if (i == 0) "Today" else dayFormat.format(c.time)
            val sum = allSales.filter { it.saleDate == dateStr }.sumOf { it.finalAmount }
            dayTotals.add(sum)
            list.add(Triple(dayLabel, sum, 0f))
        }
        val maxVal = (dayTotals.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        list.map { Triple(it.first, it.second, (it.second / maxVal).toFloat()) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_scroll_column"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Top Greeting Header
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HALAL CHICKEN SHOP HANTI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = EmeraldLight
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Good Day, Mr. Sajid",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Today is $formattedToday",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onNavigate("notifications") },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("dashboard_notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = CrimsonDanger) {
                                    Text("3", color = Color.White)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Financial Metrics Grid
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Top Row: Total Sales & Net Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Total Sales",
                        value = formatCurrency(totalSales),
                        icon = Icons.Default.TrendingUp,
                        iconColor = EmeraldLight,
                        modifier = Modifier.weight(1f).testTag("stat_total_sales")
                    )

                    MetricStatCard(
                        title = "Net Profit",
                        value = formatCurrency(netProfit),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = GoldAccent,
                        modifier = Modifier.weight(1f).testTag("stat_net_profit")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Second Row: Cash Received & UPI Received
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Cash Received",
                        value = formatCurrency(cashReceived),
                        icon = Icons.Default.Payments,
                        iconColor = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "UPI Received",
                        value = formatCurrency(upiReceived),
                        icon = Icons.Default.QrCodeScanner,
                        iconColor = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Third Row: Pending Today & Total Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Pending Today",
                        value = formatCurrency(pendingToday),
                        icon = Icons.Default.HourglassTop,
                        iconColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Total Expenses",
                        value = formatCurrency(totalExpenses),
                        icon = Icons.Default.ReceiptLong,
                        iconColor = CrimsonDanger,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (totalOutstanding > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("khata") },
                        colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Outstanding Receivables: ${formatCurrency(totalOutstanding)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Khata",
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Stock Summary Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Live Chicken",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${liveChickenStats.second.toInt()} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldLight
                        )
                        Text(
                            text = "${liveChickenStats.first} birds",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chicken Sold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${chickenSoldStats.first} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        Text(
                            text = "${chickenSoldStats.second} pieces",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Eggs Sold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${eggsSoldStats.third}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF60A5FA)
                        )
                        Text(
                            text = "eggs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3B. Price Management Quick Card
        item {
            val chickenRate = settings?.defaultChickenRate ?: 300.0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f))
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
                            text = "CURRENT CHICKEN RATE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldLight
                        )
                        Text(
                            text = "₹${chickenRate.toInt()} / KG",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val historyText = settings?.chickenPriceHistory ?: ""
                        if (historyText.isNotBlank()) {
                            Text(
                                text = "History: ${historyText.take(28)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            newRateInput = chickenRate.toInt().toString()
                            showPriceDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Price", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 4. Quick Actions
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Sell,
                        label = "Sell Chicken",
                        iconBgColor = EmeraldContainer,
                        iconTint = EmeraldLight,
                        onClick = { onNavigate("quick_sale") },
                        modifier = Modifier.weight(1f).testTag("quick_action_sell_chicken")
                    )

                    QuickActionButton(
                        icon = Icons.Default.ShoppingBag,
                        label = "Buy Chicken",
                        iconBgColor = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                        iconTint = Color(0xFF93C5FD),
                        onClick = { onNavigate("chicken_purchase") },
                        modifier = Modifier.weight(1f).testTag("quick_action_buy_chicken")
                    )

                    QuickActionButton(
                        icon = Icons.Default.Egg,
                        label = "Sell Eggs",
                        iconBgColor = AmberContainer,
                        iconTint = AmberAccent,
                        onClick = { onNavigate("egg_management") },
                        modifier = Modifier.weight(1f).testTag("quick_action_sell_eggs")
                    )

                    QuickActionButton(
                        icon = Icons.Default.Receipt,
                        label = "Add Expense",
                        iconBgColor = CrimsonContainer,
                        iconTint = CrimsonDanger,
                        onClick = { onNavigate("expenses") },
                        modifier = Modifier.weight(1f).testTag("quick_action_add_expense")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Payments,
                        label = "Receive Payment",
                        iconBgColor = Color(0xFF065F46).copy(alpha = 0.5f),
                        iconTint = Color(0xFF34D399),
                        onClick = { onNavigate("khata") },
                        modifier = Modifier.weight(1f).testTag("quick_action_receive_payment")
                    )

                    QuickActionButton(
                        icon = Icons.Default.LocalShipping,
                        label = "Business Delivery",
                        iconBgColor = Color(0xFF581C87).copy(alpha = 0.4f),
                        iconTint = Color(0xFFC084FC),
                        onClick = { onNavigate("business_customers") },
                        modifier = Modifier.weight(1f).testTag("quick_action_business_delivery")
                    )

                    QuickActionButton(
                        icon = Icons.Default.Calculate,
                        label = "Yield / Process",
                        iconBgColor = Color(0xFF374151),
                        iconTint = Color(0xFFE5E7EB),
                        onClick = { onNavigate("chicken_processing") },
                        modifier = Modifier.weight(1f).testTag("quick_action_yield_process")
                    )

                    QuickActionButton(
                        icon = Icons.Default.LockClock,
                        label = "Close Day",
                        iconBgColor = Color(0xFF78350F).copy(alpha = 0.5f),
                        iconTint = GoldAccent,
                        onClick = { onNavigate("daily_closing") },
                        modifier = Modifier.weight(1f).testTag("quick_action_close_day")
                    )
                }
            }
        }

        // 5. 7-Day Performance Chart
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Sales Trend",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Weekly",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean Bar Chart using Compose Layout
                    if (weeklyData.all { it.second == 0.0 }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No weekly sales recorded yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyData.forEach { (day, amount, ratio) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .fillMaxHeight(fraction = ratio.coerceIn(0.08f, 1.0f) * 0.85f)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (day == "Today") EmeraldLight else EmeraldDark.copy(alpha = 0.7f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (day == "Today") EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Institutional Scheduled Alert
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Delhi Public School Biraul (DPS)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Sunday & Wednesday Scheduled Delivery",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigate("business_customers") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Deliver", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 7. Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Sales",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onNavigate("sales") }) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldLight
                    )
                }
            }
        }

        // Recent sales list items
        if (recentSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No sales recorded yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigate("quick_sale") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Sale", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            items(recentSales.take(4)) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (sale.productType == "Chicken") EmeraldContainer else AmberContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (sale.productType == "Chicken") Icons.Default.Restaurant else Icons.Default.Egg,
                                    contentDescription = null,
                                    tint = if (sale.productType == "Chicken") EmeraldLight else AmberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${sale.productType} (${sale.chickenCut})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sale.customerName} • ${sale.quantity} ${sale.quantityUnit} @ ₹${sale.sellingRate.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(sale.finalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StatusBadge(status = sale.paymentMethod)
                        }
                    }
                }
            }
        }
    }

    if (showPriceDialog) {
        val currentRate = settings?.defaultChickenRate ?: 300.0
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = {
                Text("Price Management", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Current Chicken Selling Rate: ₹${currentRate.toInt()} / KG",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Changing this rate will automatically apply to all new sales. Past completed sales preserve their original historical rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newRateInput,
                        onValueChange = { newRateInput = it },
                        label = { Text("New Selling Rate (₹ / KG)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    val history = settings?.chickenPriceHistory ?: ""
                    if (history.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rate History: $history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = newRateInput.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            viewModel.updateChickenSellingRate(rate)
                            showPriceDialog = false
                            Toast.makeText(context, "Rate updated to ₹${rate.toInt()}/KG", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid rate", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Rate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
