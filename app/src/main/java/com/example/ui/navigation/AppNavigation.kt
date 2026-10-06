package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentRecordEntity
import com.example.ui.screens.*
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.HalalShopViewModel

enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("dashboard", "Home", Icons.Default.Home),
    SALES("sales", "Sales", Icons.Default.Receipt),
    STOCK("stock_menu", "Stock", Icons.Default.Inventory2),
    KHATA("khata", "Khata", Icons.Default.AccountBalanceWallet),
    MORE("more_menu", "More", Icons.Default.Menu)
}

@Composable
fun AppNavigation(viewModel: HalalShopViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val lastPaymentSuccess by viewModel.lastPaymentSuccess.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()

    var activeBottomTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var screenHistory by remember { mutableStateOf(listOf("dashboard")) }

    fun navigateToScreen(screen: String) {
        screenHistory = screenHistory + screen
        viewModel.navigateTo(screen)

        // Sync bottom bar highlight
        when (screen) {
            "dashboard" -> activeBottomTab = BottomNavItem.HOME
            "sales" -> activeBottomTab = BottomNavItem.SALES
            "chicken_processing", "chicken_purchase", "stock_menu", "egg_management" -> activeBottomTab = BottomNavItem.STOCK
            "khata" -> activeBottomTab = BottomNavItem.KHATA
            "more_menu", "reports", "expenses", "daily_closing", "business_customers", "settings", "about" -> activeBottomTab = BottomNavItem.MORE
        }
    }

    fun navigateBack() {
        if (screenHistory.size > 1) {
            val updated = screenHistory.dropLast(1)
            screenHistory = updated
            val prev = updated.last()
            viewModel.navigateTo(prev)
            when (prev) {
                "dashboard" -> activeBottomTab = BottomNavItem.HOME
                "sales" -> activeBottomTab = BottomNavItem.SALES
                "stock_menu" -> activeBottomTab = BottomNavItem.STOCK
                "khata" -> activeBottomTab = BottomNavItem.KHATA
                "more_menu" -> activeBottomTab = BottomNavItem.MORE
            }
        } else {
            viewModel.navigateTo("dashboard")
            activeBottomTab = BottomNavItem.HOME
        }
    }

    // Handle Back Press on secondary screens
    if (currentScreen != "dashboard" && currentScreen != "login" && currentScreen != "splash") {
        BackHandler {
            navigateBack()
        }
    }

    // Top-level switch
    when (currentScreen) {
        "splash" -> {
            SplashScreen(
                onSplashFinished = {
                    viewModel.navigateTo("login")
                }
            )
        }

        "login" -> {
            LoginScreen(
                onLoginSuccess = {
                    navigateToScreen("dashboard")
                },
                onValidatePin = { enteredPin ->
                    viewModel.loginWithPin(enteredPin)
                }
            )
        }

        else -> {
            // Main Authenticated Shell with Bottom Navigation Bar
            val showBottomNav = currentScreen in listOf("dashboard", "sales", "stock_menu", "khata", "more_menu")

            Scaffold(
                bottomBar = {
                    if (showBottomNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            BottomNavItem.values().forEach { item ->
                                val isSelected = activeBottomTab == item
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        activeBottomTab = item
                                        navigateToScreen(item.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${item.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else 0.dp)
                ) {
                    when (currentScreen) {
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigate = { destination ->
                                    navigateToScreen(destination)
                                }
                            )
                        }

                        "sales" -> {
                            DailySalesScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onNewSaleClick = { navigateToScreen("quick_sale") }
                            )
                        }

                        "quick_sale" -> {
                            QuickSaleScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onSaleSaved = { navigateBack() }
                            )
                        }

                        "chicken_purchase" -> {
                            LiveChickenPurchaseScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onPurchaseSaved = { navigateBack() }
                            )
                        }

                        "chicken_processing" -> {
                            ChickenProcessingScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onProcessingSaved = { navigateBack() }
                            )
                        }

                        "stock_menu" -> {
                            StockMenuScreen(
                                onNavigate = { destination -> navigateToScreen(destination) }
                            )
                        }

                        "egg_management" -> {
                            EggManagementScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        "khata" -> {
                            PendingPaymentsScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onCustomerClick = { customerId ->
                                    val customer = viewModel.customers.value.firstOrNull { it.id == customerId }
                                    if (customer != null) {
                                        viewModel.selectCustomer(customer)
                                        navigateToScreen("customer_ledger")
                                    }
                                },
                                onPaymentSuccess = { record ->
                                    navigateToScreen("payment_success")
                                }
                            )
                        }

                        "customer_ledger" -> {
                            val cust = selectedCustomer ?: CustomerEntity(name = "Customer Ledger")
                            CustomerLedgerScreen(
                                customer = cust,
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onPaymentSuccess = {
                                    navigateToScreen("payment_success")
                                }
                            )
                        }

                        "payment_success" -> {
                            val record = lastPaymentSuccess ?: PaymentRecordEntity(
                                customerId = 1,
                                customerName = "Customer",
                                amountReceived = 5000.0,
                                previousBalance = 5000.0,
                                remainingBalance = 0.0,
                                paymentMethod = "UPI",
                                paymentDate = "Today"
                            )
                            PaymentSuccessScreen(
                                paymentRecord = record,
                                onViewLedger = {
                                    val cust = viewModel.customers.value.firstOrNull { it.id == record.customerId }
                                    if (cust != null) viewModel.selectCustomer(cust)
                                    navigateToScreen("customer_ledger")
                                },
                                onDone = {
                                    navigateToScreen("dashboard")
                                }
                            )
                        }

                        "business_customers" -> {
                            BusinessCustomersScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onSelectCustomer = { cust ->
                                    viewModel.selectCustomer(cust)
                                    navigateToScreen("customer_ledger")
                                }
                            )
                        }

                        "daily_closing" -> {
                            DailyClosingScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        "expenses" -> {
                            ExpenseScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        "reports" -> {
                            ReportsAnalyticsScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        "notifications" -> {
                            NotificationsScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() }
                            )
                        }

                        "settings" -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = { navigateBack() },
                                onNavigateAbout = { navigateToScreen("about") },
                                onLogout = {
                                    viewModel.logout()
                                    viewModel.navigateTo("login")
                                }
                            )
                        }

                        "about" -> {
                            AboutScreen(
                                onBackClick = { navigateBack() }
                            )
                        }

                        "more_menu" -> {
                            MoreMenuScreen(
                                onNavigate = { destination -> navigateToScreen(destination) }
                            )
                        }
                    }
                }
            }
        }
    }
}
