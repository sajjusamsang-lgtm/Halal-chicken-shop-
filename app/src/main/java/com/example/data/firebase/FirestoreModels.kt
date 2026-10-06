package com.example.data.firebase

import com.google.firebase.Timestamp

data class ShopDoc(
    val shopId: String = "",
    val ownerId: String = "",
    val shopName: String = "HALAL CHICKEN SHOP HANTI",
    val ownerName: String = "Mr. Sajid",
    val phone: String = "+91 9876543210",
    val address: String = "Hanti, Bihar, India",
    val defaultChickenRate: Double = 220.0,
    val defaultEggTrayRate: Double = 180.0,
    val invoicePrefix: String = "HCSH",
    val allowNegativeStock: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class CustomerDoc(
    val id: String = "",
    val shopId: String = "",
    val name: String = "",
    val phone: String = "",
    val businessName: String = "",
    val customerType: String = "RETAIL", // RETAIL, SCHOOL, HOTEL, RESTAURANT
    val paymentCycle: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val defaultRate: Double = 220.0,
    val deliveryDays: String = "", // e.g. "Sunday, Wednesday" for DPS
    val totalPending: Double = 0.0,
    val notes: String = "",
    val status: String = "ACTIVE",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class LiveChickenPurchaseDoc(
    val id: String = "",
    val shopId: String = "",
    val batchCode: String = "",
    val supplierName: String = "",
    val supplierPhone: String = "",
    val numberOfBirds: Int = 0,
    val totalLiveWeightKg: Double = 0.0,
    val avgWeightPerBirdKg: Double = 0.0,
    val purchaseRatePerKg: Double = 0.0,
    val purchaseAmount: Double = 0.0,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val paymentStatus: String = "PAID",
    val paymentMethod: String = "CASH",
    val purchaseDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, CANCELLED
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ProcessingRecordDoc(
    val id: String = "",
    val shopId: String = "",
    val batchCode: String = "",
    val purchaseId: String = "",
    val liveBirdCount: Int = 0,
    val liveWeightKg: Double = 0.0,
    val usableChickenWeightKg: Double = 0.0,
    val wasteOffalWeightKg: Double = 0.0,
    val otherLossKg: Double = 0.0,
    val yieldPercentage: Double = 0.0,
    val lossPercentage: Double = 0.0,
    val totalBatchCost: Double = 0.0,
    val costPerUsableKg: Double = 0.0,
    val processingDate: String = "",
    val status: String = "ACTIVE",
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

data class SaleDoc(
    val id: String = "",
    val shopId: String = "",
    val billNumber: String = "",
    val customerId: String? = null,
    val customerName: String = "",
    val customerType: String = "RETAIL",
    val productType: String = "CHICKEN",
    val chickenCut: String = "Whole Chicken",
    val quantityUnit: String = "KG",
    val quantity: Double = 0.0,
    val piecesCount: Int = 0,
    val sellingRate: Double = 0.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val costAmount: Double = 0.0,
    val grossProfit: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentStatus: String = "PAID", // PAID, PARTIAL, UNPAID
    val amountPaid: Double = 0.0,
    val balancePending: Double = 0.0,
    val saleDate: String = "",
    val dueDate: String = "",
    val isInstitutionalDelivery: Boolean = false,
    val isDelivered: Boolean = true,
    val status: String = "ACTIVE", // ACTIVE, CANCELLED
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class PaymentRecordDoc(
    val id: String = "",
    val shopId: String = "",
    val idempotencyKey: String = "", // Prevents duplicate payment submissions
    val saleId: String? = null,
    val billNumber: String? = null,
    val customerId: String = "",
    val customerName: String = "",
    val amountReceived: Double = 0.0,
    val previousBalance: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, UPI, BANK_TRANSFER
    val referenceId: String = "",
    val paymentDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, CANCELLED
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

data class ExpenseDoc(
    val id: String = "",
    val shopId: String = "",
    val category: String = "Transport", // Transport, Electricity, Rent, Packaging, Labour, Ice, Cleaning, Fuel, Other
    val description: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val expenseDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, CANCELLED
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class EggTransactionDoc(
    val id: String = "",
    val shopId: String = "",
    val transactionType: String = "SALE", // PURCHASE, SALE, WASTAGE, ADJUSTMENT
    val eggType: String = "BROWN", // BROWN, WHITE
    val unit: String = "TRAY", // TRAY (30 eggs), CARDBOARD (210 eggs / 7 trays), LOOSE (individual)
    val unitQuantity: Double = 0.0,
    val totalEggsCount: Int = 0, // INDIVIDUAL EGGS as base stock unit
    val ratePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val costAmount: Double = 0.0,
    val grossProfit: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentStatus: String = "PAID",
    val amountPaid: Double = 0.0,
    val customerOrSupplierName: String = "",
    val date: String = "",
    val status: String = "ACTIVE",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

data class StockMovementDoc(
    val id: String = "",
    val shopId: String = "",
    val type: String = "PURCHASE", // PURCHASE, PROCESSING, SALE, WASTAGE, ADJUSTMENT
    val category: String = "CHICKEN", // CHICKEN, EGG
    val quantity: Double = 0.0,
    val unit: String = "KG", // KG, PIECES, EGGS
    val baseQuantityEggs: Int = 0, // Individual eggs (1 tray = 30 eggs, 1 cardboard = 210 eggs)
    val referenceId: String = "",
    val referenceType: String = "",
    val date: String = "",
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

data class DailyClosingDoc(
    val id: String = "",
    val shopId: String = "",
    val date: String = "",
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
    val notes: String = "",
    val createdAt: Timestamp? = null
)

data class WastageDoc(
    val id: String = "",
    val shopId: String = "",
    val productType: String = "Chicken",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val reason: String = "",
    val estimatedCost: Double = 0.0,
    val date: String = "",
    val status: String = "ACTIVE",
    val notes: String = "",
    val createdAt: Timestamp? = null
)

data class AuditLogDoc(
    val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val action: String = "", // SALE_CREATED, SALE_CANCELLED, PAYMENT_CREATED, EXPENSE_CREATED, etc.
    val recordType: String = "",
    val recordId: String = "",
    val details: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val createdAt: Timestamp? = null
)

data class IntegrityIssue(
    val severity: String = "PASS", // PASS, WARNING, ERROR
    val code: String = "",
    val message: String = "",
    val details: String = ""
)

data class DataIntegrityReport(
    val overallStatus: String = "PASS", // PASS, WARNING, ERROR
    val totalChecked: Int = 0,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val issues: List<IntegrityIssue> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
