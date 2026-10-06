package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ErpTopBar
import com.example.ui.theme.*

@Composable
fun StockMenuScreen(
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Inventory & Stock",
                subtitle = "Poultry, Processing & Eggs"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MenuFeatureCard(
                    title = "Live Chicken Purchase",
                    subtitle = "Log incoming batches, suppliers, bird count & live weight",
                    icon = Icons.Default.ShoppingBag,
                    iconBg = EmeraldContainer,
                    iconTint = EmeraldLight,
                    onClick = { onNavigate("chicken_purchase") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Chicken Processing & Yield",
                    subtitle = "Track live weight vs usable dressed weight, calculate yield % and accurate usable cost/kg",
                    icon = Icons.Default.Calculate,
                    iconBg = Color(0xFF1E3A8A).copy(alpha = 0.4f),
                    iconTint = Color(0xFF93C5FD),
                    onClick = { onNavigate("chicken_processing") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Egg Management",
                    subtitle = "Brown eggs, white eggs, stock in trays (30) and cardboards (210)",
                    icon = Icons.Default.Egg,
                    iconBg = AmberContainer,
                    iconTint = AmberAccent,
                    onClick = { onNavigate("egg_management") }
                )
            }
        }
    }
}

@Composable
fun MoreMenuScreen(
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            ErpTopBar(
                title = "Business Management",
                subtitle = "HALAL CHICKEN SHOP HANTI"
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
            item {
                MenuFeatureCard(
                    title = "Institutional Customers",
                    subtitle = "Delhi Public School Biraul (DPS), Hotels & Restaurants",
                    icon = Icons.Default.School,
                    iconBg = Color(0xFF581C87).copy(alpha = 0.4f),
                    iconTint = Color(0xFFC084FC),
                    onClick = { onNavigate("business_customers") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Reports & Profit & Loss",
                    subtitle = "7-day trends, monthly statements, COGS & margin %",
                    icon = Icons.Default.Analytics,
                    iconBg = EmeraldContainer,
                    iconTint = EmeraldLight,
                    onClick = { onNavigate("reports") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Daily Closing (Day Closing)",
                    subtitle = "Reconcile daily cash and UPI, review pending and close books",
                    icon = Icons.Default.LockClock,
                    iconBg = Color(0xFF78350F).copy(alpha = 0.5f),
                    iconTint = GoldAccent,
                    onClick = { onNavigate("daily_closing") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Shop Expenses",
                    subtitle = "Ice, helper labour wages, transport, packaging & utilities",
                    icon = Icons.Default.Receipt,
                    iconBg = CrimsonContainer,
                    iconTint = CrimsonDanger,
                    onClick = { onNavigate("expenses") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Notifications & Reminders",
                    subtitle = "Scheduled deliveries, overdue payments & stock alerts",
                    icon = Icons.Default.Notifications,
                    iconBg = Color(0xFF1E293B),
                    iconTint = Color(0xFF94A3B8),
                    onClick = { onNavigate("notifications") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "Settings & Security",
                    subtitle = "Shop profile, owner PIN, rate defaults & dark/light theme",
                    icon = Icons.Default.Settings,
                    iconBg = Color(0xFF334155),
                    iconTint = Color.White,
                    onClick = { onNavigate("settings") }
                )
            }

            item {
                MenuFeatureCard(
                    title = "About System",
                    subtitle = "HALAL CHICKEN SHOP HANTI • Designed & Developed by Mr. Sajid",
                    icon = Icons.Default.Info,
                    iconBg = EmeraldContainer,
                    iconTint = GoldAccent,
                    onClick = { onNavigate("about") }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun MenuFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
