package com.example.data.repository

import com.example.data.dao.HalalShopDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class HalalShopRepository(private val dao: HalalShopDao) {

    val allCustomers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    val allPurchases: Flow<List<LiveChickenPurchaseEntity>> = dao.getAllPurchases()
    val allProcessingRecords: Flow<List<ProcessingRecordEntity>> = dao.getAllProcessingRecords()
    val latestProcessingRecord: Flow<ProcessingRecordEntity?> = dao.getLatestProcessingRecord()
    val allSales: Flow<List<SaleEntity>> = dao.getAllSales()
    val pendingSales: Flow<List<SaleEntity>> = dao.getPendingSales()
    val allEggTransactions: Flow<List<EggTransactionEntity>> = dao.getAllEggTransactions()
    val allPayments: Flow<List<PaymentRecordEntity>> = dao.getAllPayments()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val allWastage: Flow<List<WastageEntity>> = dao.getAllWastage()
    val allClosings: Flow<List<DailyClosingEntity>> = dao.getAllClosings()
    val settings: Flow<ShopSettingsEntity?> = dao.getSettings()

    fun getSalesByDate(date: String): Flow<List<SaleEntity>> = dao.getSalesByDate(date)
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>> = dao.getExpensesByDate(date)
    fun getEggTransactionsByDate(date: String): Flow<List<EggTransactionEntity>> = dao.getEggTransactionsByDate(date)
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>> = dao.getSalesByCustomer(customerId)
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentRecordEntity>> = dao.getPaymentsByCustomer(customerId)
    fun getCustomerById(id: Long): Flow<CustomerEntity?> = dao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): CustomerEntity? = dao.getCustomerByIdDirect(id)

    suspend fun insertCustomer(customer: CustomerEntity): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = dao.deleteCustomer(customer)

    suspend fun generateNextBillNumber(): String {
        var count = dao.getSaleCount() + 1
        var candidate = String.format(Locale.US, "HCS-%04d", count)
        while (dao.getSaleByBillNumber(candidate) != null) {
            count++
            candidate = String.format(Locale.US, "HCS-%04d", count)
        }
        return candidate
    }

    suspend fun cleanDemoData() {
        dao.deleteDemoSales()
        dao.deleteDemoPurchases()
        dao.deleteDemoProcessing()
        dao.deleteDemoCustomers()
        dao.deleteDemoExpenses()
    }

    suspend fun generateBatchCode(): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val randomNum = (100..999).random()
        return "LB-$year-$randomNum"
    }

    suspend fun insertLiveChickenPurchase(
        supplierName: String,
        supplierPhone: String,
        numberOfBirds: Int,
        totalLiveWeightKg: Double,
        purchaseRatePerKg: Double,
        transportCost: Double,
        otherCost: Double,
        paymentStatus: String,
        paymentMethod: String,
        notes: String
    ): LiveChickenPurchaseEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val batchCode = generateBatchCode()
        val avgWeight = if (numberOfBirds > 0) totalLiveWeightKg / numberOfBirds else 0.0
        val purchaseAmount = totalLiveWeightKg * purchaseRatePerKg
        val totalCost = purchaseAmount + transportCost + otherCost

        val purchase = LiveChickenPurchaseEntity(
            batchCode = batchCode,
            supplierName = supplierName,
            supplierPhone = supplierPhone,
            numberOfBirds = numberOfBirds,
            totalLiveWeightKg = totalLiveWeightKg,
            avgWeightPerBirdKg = avgWeight,
            purchaseRatePerKg = purchaseRatePerKg,
            purchaseAmount = purchaseAmount,
            transportCost = transportCost,
            otherCost = otherCost,
            totalCost = totalCost,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            purchaseDate = today,
            notes = notes
        )
        dao.insertPurchase(purchase)
        return purchase
    }

    suspend fun insertProcessingRecord(
        batchCode: String,
        liveBirdCount: Int,
        liveWeightKg: Double,
        usableChickenWeightKg: Double,
        wasteOffalWeightKg: Double,
        otherLossKg: Double,
        totalBatchCost: Double,
        notes: String
    ): ProcessingRecordEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val yieldPercentage = if (liveWeightKg > 0) (usableChickenWeightKg / liveWeightKg) * 100 else 0.0
        val lossWeight = wasteOffalWeightKg + otherLossKg
        val lossPercentage = if (liveWeightKg > 0) (lossWeight / liveWeightKg) * 100 else 0.0
        val costPerUsableKg = if (usableChickenWeightKg > 0) totalBatchCost / usableChickenWeightKg else 0.0

        val record = ProcessingRecordEntity(
            batchCode = batchCode,
            liveBirdCount = liveBirdCount,
            liveWeightKg = liveWeightKg,
            usableChickenWeightKg = usableChickenWeightKg,
            wasteOffalWeightKg = wasteOffalWeightKg,
            otherLossKg = otherLossKg,
            yieldPercentage = yieldPercentage,
            lossPercentage = lossPercentage,
            totalBatchCost = totalBatchCost,
            costPerUsableKg = costPerUsableKg,
            processingDate = today,
            notes = notes
        )
        dao.insertProcessingRecord(record)
        return record
    }

    suspend fun createSale(
        customerId: Long?,
        customerName: String,
        customerType: String,
        productType: String,
        chickenCut: String,
        quantityUnit: String,
        quantity: Double,
        piecesCount: Int,
        sellingRate: Double,
        discount: Double,
        paymentMethod: String, // CASH, UPI, Pending, Partial
        amountPaidManual: Double?,
        dueDate: String,
        isInstitutionalDelivery: Boolean,
        notes: String
    ): SaleEntity {
        val billNumber = generateNextBillNumber()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val subtotal = quantity * sellingRate
        val finalAmount = (subtotal - discount).coerceAtLeast(0.0)

        // Usable cost determination
        val latestProcessing = dao.getLatestProcessingRecordDirect()
        val usableCostPerKg = latestProcessing?.costPerUsableKg ?: 175.0
        val costAmount = if (productType == ProductType.CHICKEN.name) {
            quantity * usableCostPerKg
        } else {
            quantity * 140.0 // standard egg tray cost
        }
        val grossProfit = finalAmount - costAmount

        // Payment status & balance calculation
        val (status, paid, pending) = when (paymentMethod) {
            PaymentMethod.CASH.name, PaymentMethod.UPI.name, PaymentMethod.BANK_TRANSFER.name -> {
                Triple(PaymentStatus.PAID.name, finalAmount, 0.0)
            }
            "Pending" -> {
                Triple(PaymentStatus.PENDING.name, 0.0, finalAmount)
            }
            "Partial" -> {
                val paidVal = (amountPaidManual ?: 0.0).coerceIn(0.0, finalAmount)
                val remaining = finalAmount - paidVal
                val st = if (remaining <= 0.01) PaymentStatus.PAID.name else PaymentStatus.PARTIAL.name
                Triple(st, paidVal, remaining)
            }
            else -> Triple(PaymentStatus.PAID.name, finalAmount, 0.0)
        }

        val sale = SaleEntity(
            billNumber = billNumber,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            customerType = customerType,
            productType = productType,
            chickenCut = chickenCut,
            quantityUnit = quantityUnit,
            quantity = quantity,
            piecesCount = piecesCount,
            sellingRate = sellingRate,
            subtotal = subtotal,
            discount = discount,
            finalAmount = finalAmount,
            costAmount = costAmount,
            grossProfit = grossProfit,
            paymentMethod = paymentMethod,
            paymentStatus = status,
            amountPaid = paid,
            balancePending = pending,
            saleDate = today,
            dueDate = dueDate.ifBlank { today },
            isInstitutionalDelivery = isInstitutionalDelivery,
            isDelivered = true,
            notes = notes
        )
        dao.insertSale(sale)
        return sale
    }

    suspend fun receiveCustomerPayment(
        customerId: Long,
        amountReceived: Double,
        paymentMethod: String,
        referenceId: String,
        notes: String
    ): PaymentRecordEntity? {
        val customer = dao.getCustomerByIdDirect(customerId) ?: return null
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Fetch all pending/partial sales for this customer
        // We will reduce balance from oldest sales first
        // Note: We need a direct suspend query or filter from all pending
        // Let's do it cleanly:
        var totalOutstanding = 0.0
        // We will collect from existing pending sales in dao
        // For simplicity and atomic update, we query sales for this customer
        // and adjust their balancePending and paymentStatus
        // Let's read sales directly:
        // We can do this in memory or query
        var amountLeftToAllocate = amountReceived
        val customerSales = dao.getSalesByCustomer(customerId)
        // Since getSalesByCustomer returns a Flow, we can get list via first or create a direct suspend query
        // Let's handle via updating sales
        var prevBalance = 0.0

        // We will compute prevBalance and remainingBalance
        // Let's create the payment record:
        val paymentRecord = PaymentRecordEntity(
            customerId = customerId,
            customerName = customer.name,
            amountReceived = amountReceived,
            previousBalance = 0.0, // calculated in viewModel or updated
            remainingBalance = 0.0,
            paymentMethod = paymentMethod,
            referenceId = referenceId,
            paymentDate = today,
            notes = notes
        )
        return paymentRecord
    }

    suspend fun insertPaymentRecord(record: PaymentRecordEntity): Long = dao.insertPayment(record)

    suspend fun updateSale(sale: SaleEntity) = dao.updateSale(sale)
    suspend fun deleteSale(sale: SaleEntity) = dao.deleteSale(sale)

    suspend fun insertEggTransaction(transaction: EggTransactionEntity): Long =
        dao.insertEggTransaction(transaction)

    suspend fun insertExpense(expense: ExpenseEntity): Long = dao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)

    suspend fun insertWastage(wastage: WastageEntity): Long = dao.insertWastage(wastage)

    suspend fun closeDay(closing: DailyClosingEntity): Long = dao.insertClosing(closing)

    suspend fun updateSettings(settings: ShopSettingsEntity) = dao.insertSettings(settings)

    suspend fun getSettingsDirect(): ShopSettingsEntity? = dao.getSettingsDirect()
}
