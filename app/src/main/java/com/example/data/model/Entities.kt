package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val businessName: String = "",
    val customerType: String = CustomerType.RETAIL.name,
    val address: String = "",
    val paymentCycle: String = PaymentCycle.DAILY.name,
    val defaultChickenRate: Double = 300.0,
    val deliveryDays: String = "", // e.g. "Sunday, Wednesday"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "live_chicken_purchases")
data class LiveChickenPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchCode: String,
    val supplierName: String,
    val supplierPhone: String = "",
    val numberOfBirds: Int,
    val totalLiveWeightKg: Double,
    val avgWeightPerBirdKg: Double,
    val purchaseRatePerKg: Double,
    val purchaseAmount: Double,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalCost: Double,
    val paymentStatus: String = PaymentStatus.PAID.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val purchaseDate: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "processing_records")
data class ProcessingRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchCode: String,
    val liveBirdCount: Int,
    val liveWeightKg: Double,
    val usableChickenWeightKg: Double,
    val wasteOffalWeightKg: Double,
    val otherLossKg: Double = 0.0,
    val yieldPercentage: Double,
    val lossPercentage: Double,
    val totalBatchCost: Double,
    val costPerUsableKg: Double,
    val processingDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerType: String = CustomerType.RETAIL.name,
    val productType: String = ProductType.CHICKEN.name,
    val chickenCut: String = ChickenCut.WHOLE.name,
    val quantityUnit: String = WeightUnit.KG.name,
    val quantity: Double,
    val piecesCount: Int = 0,
    val sellingRate: Double,
    val subtotal: Double,
    val discount: Double = 0.0,
    val finalAmount: Double,
    val costAmount: Double = 0.0,
    val grossProfit: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val paymentStatus: String = PaymentStatus.PAID.name,
    val amountPaid: Double,
    val balancePending: Double = 0.0,
    val saleDate: String, // YYYY-MM-DD
    val dueDate: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isInstitutionalDelivery: Boolean = false,
    val isDelivered: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "egg_transactions")
data class EggTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionType: String, // "PURCHASE" or "SALE"
    val eggType: String = EggType.BROWN.name,
    val unit: String = EggUnit.TRAY.name,
    val unitQuantity: Double,
    val totalEggsCount: Int,
    val ratePerUnit: Double,
    val totalAmount: Double,
    val costAmount: Double = 0.0,
    val grossProfit: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val paymentStatus: String = PaymentStatus.PAID.name,
    val amountPaid: Double = 0.0,
    val customerOrSupplierName: String = "",
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long? = null,
    val billNumber: String? = null,
    val customerId: Long,
    val customerName: String,
    val amountReceived: Double,
    val previousBalance: Double,
    val remainingBalance: Double,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val referenceId: String = "",
    val paymentDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val expenseDate: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wastage_records")
data class WastageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productType: String,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val estimatedCost: Double,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "daily_closings")
data class DailyClosingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val cashPaymentsReceived: Double = 0.0,
    val cashExpenses: Double = 0.0,
    val cashPurchasePayments: Double = 0.0,
    val closingCash: Double = 0.0,
    val openingUpi: Double = 0.0,
    val upiSales: Double = 0.0,
    val upiReceived: Double = 0.0,
    val upiPayments: Double = 0.0,
    val closingUpi: Double = 0.0,
    val newPending: Double = 0.0,
    val collectedPending: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val isClosed: Boolean = false,
    val closedTimestamp: Long = 0L,
    val notes: String = ""
)

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "HALAL CHICKEN SHOP HANTI",
    val ownerName: String = "Mr. Sajid",
    val phone: String = "+91 9708099035",
    val address: String = "Hanti, Bihar, India",
    val invoicePrefix: String = "HCS",
    val defaultChickenRate: Double = 300.0,
    val defaultEggTrayRateBrown: Double = 180.0,
    val defaultEggTrayRateWhite: Double = 170.0,
    val biometricEnabled: Boolean = true,
    val ownerPin: String = "9708099035",
    val isDarkMode: Boolean = true,
    val chickenPriceHistory: String = "01 Oct — ₹300/KG"
)
