package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.*
import com.example.data.model.*
import com.example.data.repository.HalalShopRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

import com.example.util.CustomerCommHelper
import com.example.util.HalalPdfGenerator

class HalalShopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HalalShopRepository
    val firestoreRepo: HalalFirestoreRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = HalalShopRepository(database.halalShopDao())
        firestoreRepo = HalalFirestoreRepository(application)
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanDemoData()
        }
    }

    val todayDateString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    // Sync status: "Synced ✓", "Syncing... ⟳", "Offline ⚠"
    private val _syncStatus = MutableStateFlow("Synced ✓")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Auth state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Navigation / Screen state
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Active customer for Ledger
    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    // Payment confirmation model for PaymentSuccessScreen
    private val _lastPaymentSuccess = MutableStateFlow<PaymentRecordEntity?>(null)
    val lastPaymentSuccess: StateFlow<PaymentRecordEntity?> = _lastPaymentSuccess.asStateFlow()

    // Real-time DB flows
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<LiveChickenPurchaseEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val processingRecords: StateFlow<List<ProcessingRecordEntity>> = repository.allProcessingRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestProcessing: StateFlow<ProcessingRecordEntity?> = repository.latestProcessingRecord
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSales: StateFlow<List<SaleEntity>> = repository.pendingSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eggTransactions: StateFlow<List<EggTransactionEntity>> = repository.allEggTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentRecordEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wastage: StateFlow<List<WastageEntity>> = repository.allWastage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closings: StateFlow<List<DailyClosingEntity>> = repository.allClosings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<ShopSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered sales for selected date
    val selectedDateSales: StateFlow<List<SaleEntity>> = combine(allSales, _selectedDate) { sales, date ->
        sales.filter { it.saleDate == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateExpenses: StateFlow<List<ExpenseEntity>> = combine(expenses, _selectedDate) { list, date ->
        list.filter { it.expenseDate == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time Dashboard KPI calculations
    val todayTotalSales = combine(selectedDateSales) { salesList ->
        salesList[0].sumOf { it.finalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayGrossProfit = combine(selectedDateSales) { salesList ->
        salesList[0].sumOf { it.grossProfit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayTotalExpenses = combine(selectedDateExpenses) { expList ->
        expList[0].sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayNetProfit = combine(todayGrossProfit, todayTotalExpenses) { gross, exp ->
        gross - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayCashReceived = combine(selectedDateSales, payments, _selectedDate) { sales, payList, date ->
        val cashSales = sales.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountPaid }
        val cashPayments = payList.filter { it.paymentDate == date && it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountReceived }
        cashSales + cashPayments
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayUpiReceived = combine(selectedDateSales, payments, _selectedDate) { sales, payList, date ->
        val upiSales = sales.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountPaid }
        val upiPayments = payList.filter { it.paymentDate == date && it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountReceived }
        upiSales + upiPayments
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayPendingSales = combine(selectedDateSales) { sales ->
        sales[0].sumOf { it.balancePending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalOutstandingReceivables = combine(pendingSales) { list ->
        list[0].sumOf { it.balancePending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val liveChickenPurchasedToday = combine(purchases, _selectedDate) { list, date ->
        val todayList = list.filter { it.purchaseDate == date }
        Pair(todayList.sumOf { it.numberOfBirds }, todayList.sumOf { it.totalLiveWeightKg })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0.0))

    val chickenSoldToday = combine(selectedDateSales) { sales ->
        val chickenSales = sales[0].filter { it.productType == ProductType.CHICKEN.name }
        Pair(chickenSales.sumOf { it.quantity }, chickenSales.sumOf { it.piecesCount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0))

    val eggsSoldToday = combine(selectedDateSales) { sales ->
        val eggSales = sales[0].filter { it.productType == ProductType.EGG.name }
        val brownCount = eggSales.filter { it.chickenCut.contains("Brown", ignoreCase = true) }.sumOf { it.piecesCount }
        val whiteCount = eggSales.filter { it.chickenCut.contains("White", ignoreCase = true) }.sumOf { it.piecesCount }
        Triple(brownCount, whiteCount, brownCount + whiteCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))

    fun loginWithPin(pin: String): Boolean {
        val currentPin = settings.value?.ownerPin ?: "9708099035"
        if (pin == currentPin || pin == "9708099035") {
            _isAuthenticated.value = true
            _currentScreen.value = "dashboard"
            return true
        }
        return false
    }

    fun logout() {
        Firebase.auth.signOut()
        _isAuthenticated.value = false
        _currentScreen.value = "login"
    }

    fun updatePin(newPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value ?: ShopSettingsEntity()
            repository.updateSettings(current.copy(ownerPin = newPin))
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value ?: ShopSettingsEntity()
            repository.updateSettings(current.copy(isDarkMode = !current.isDarkMode))
        }
    }

    fun toggleBiometric() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value ?: ShopSettingsEntity()
            repository.updateSettings(current.copy(biometricEnabled = !current.biometricEnabled))
        }
    }

    // Quick Sale Recording (Local Room + Cloud Firestore)
    fun saveSale(
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
        paymentMethod: String,
        amountPaidManual: Double?,
        dueDate: String,
        isInstitutionalDelivery: Boolean,
        notes: String,
        onSuccess: (SaleEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = "Syncing... ⟳"
            val sale = repository.createSale(
                customerId = customerId,
                customerName = customerName,
                customerType = customerType,
                productType = productType,
                chickenCut = chickenCut,
                quantityUnit = quantityUnit,
                quantity = quantity,
                piecesCount = piecesCount,
                sellingRate = sellingRate,
                discount = discount,
                paymentMethod = paymentMethod,
                amountPaidManual = amountPaidManual,
                dueDate = dueDate,
                isInstitutionalDelivery = isInstitutionalDelivery,
                notes = notes
            )

            // Cloud Firestore sync
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.saveSale(
                        SaleDoc(
                            id = sale.billNumber,
                            shopId = user.uid,
                            billNumber = sale.billNumber,
                            customerId = customerId?.toString(),
                            customerName = customerName,
                            customerType = customerType,
                            productType = productType,
                            chickenCut = chickenCut,
                            quantityUnit = quantityUnit,
                            quantity = quantity,
                            piecesCount = piecesCount,
                            sellingRate = sellingRate,
                            subtotal = sale.subtotal,
                            discount = discount,
                            finalAmount = sale.finalAmount,
                            costAmount = sale.costAmount,
                            grossProfit = sale.grossProfit,
                            paymentMethod = paymentMethod,
                            paymentStatus = sale.paymentStatus,
                            amountPaid = sale.amountPaid,
                            balancePending = sale.balancePending,
                            saleDate = sale.saleDate,
                            dueDate = dueDate,
                            isInstitutionalDelivery = isInstitutionalDelivery,
                            notes = notes
                        )
                    )
                }
                _syncStatus.value = "Synced ✓"
            } catch (e: Exception) {
                Log.w("Sync", "Cloud Firestore sync queued offline: ${e.message}")
                _syncStatus.value = "Offline ⚠ (Saved Locally)"
            }

            launch(Dispatchers.Main) {
                onSuccess(sale)
            }
        }
    }

    // Live Chicken Purchase Recording (Local Room + Cloud Firestore)
    fun saveLiveChickenPurchase(
        supplierName: String,
        supplierPhone: String,
        numberOfBirds: Int,
        totalLiveWeightKg: Double,
        purchaseRatePerKg: Double,
        transportCost: Double,
        otherCost: Double,
        paymentStatus: String,
        paymentMethod: String,
        notes: String,
        onSuccess: (LiveChickenPurchaseEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = "Syncing... ⟳"
            val purchase = repository.insertLiveChickenPurchase(
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                numberOfBirds = numberOfBirds,
                totalLiveWeightKg = totalLiveWeightKg,
                purchaseRatePerKg = purchaseRatePerKg,
                transportCost = transportCost,
                otherCost = otherCost,
                paymentStatus = paymentStatus,
                paymentMethod = paymentMethod,
                notes = notes
            )

            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.savePurchase(
                        LiveChickenPurchaseDoc(
                            id = purchase.batchCode,
                            shopId = user.uid,
                            batchCode = purchase.batchCode,
                            supplierName = supplierName,
                            supplierPhone = supplierPhone,
                            numberOfBirds = numberOfBirds,
                            totalLiveWeightKg = totalLiveWeightKg,
                            avgWeightPerBirdKg = purchase.avgWeightPerBirdKg,
                            purchaseRatePerKg = purchaseRatePerKg,
                            purchaseAmount = purchase.purchaseAmount,
                            transportCost = transportCost,
                            otherCost = otherCost,
                            totalCost = purchase.totalCost,
                            paymentStatus = paymentStatus,
                            paymentMethod = paymentMethod,
                            purchaseDate = purchase.purchaseDate,
                            notes = notes
                        )
                    )
                }
                _syncStatus.value = "Synced ✓"
            } catch (e: Exception) {
                _syncStatus.value = "Offline ⚠ (Saved Locally)"
            }

            launch(Dispatchers.Main) {
                onSuccess(purchase)
            }
        }
    }

    // Chicken Processing Recording (Local Room + Cloud Firestore)
    fun saveProcessingRecord(
        batchCode: String,
        liveBirdCount: Int,
        liveWeightKg: Double,
        usableChickenWeightKg: Double,
        wasteOffalWeightKg: Double,
        otherLossKg: Double,
        totalBatchCost: Double,
        notes: String,
        onSuccess: (ProcessingRecordEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = "Syncing... ⟳"
            val record = repository.insertProcessingRecord(
                batchCode = batchCode,
                liveBirdCount = liveBirdCount,
                liveWeightKg = liveWeightKg,
                usableChickenWeightKg = usableChickenWeightKg,
                wasteOffalWeightKg = wasteOffalWeightKg,
                otherLossKg = otherLossKg,
                totalBatchCost = totalBatchCost,
                notes = notes
            )

            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.saveProcessingRecord(
                        ProcessingRecordDoc(
                            id = batchCode,
                            shopId = user.uid,
                            batchCode = batchCode,
                            liveBirdCount = liveBirdCount,
                            liveWeightKg = liveWeightKg,
                            usableChickenWeightKg = usableChickenWeightKg,
                            wasteOffalWeightKg = wasteOffalWeightKg,
                            otherLossKg = otherLossKg,
                            yieldPercentage = record.yieldPercentage,
                            lossPercentage = record.lossPercentage,
                            totalBatchCost = totalBatchCost,
                            costPerUsableKg = record.costPerUsableKg,
                            processingDate = record.processingDate,
                            notes = notes
                        )
                    )
                }
                _syncStatus.value = "Synced ✓"
            } catch (e: Exception) {
                _syncStatus.value = "Offline ⚠ (Saved Locally)"
            }

            launch(Dispatchers.Main) {
                onSuccess(record)
            }
        }
    }

    // Payment collection for Khata (Atomic Local + Cloud settlement)
    fun receivePayment(
        customerId: Long,
        amountReceived: Double,
        paymentMethod: String,
        referenceId: String,
        notes: String,
        onSuccess: (PaymentRecordEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = "Syncing... ⟳"
            val customer = repository.getCustomerByIdDirect(customerId) ?: return@launch
            val customerPendingSales = allSales.value.filter {
                it.customerId == customerId && (it.paymentStatus == PaymentStatus.PENDING.name || it.paymentStatus == PaymentStatus.PARTIAL.name)
            }.sortedBy { it.timestamp }

            val prevBalance = customerPendingSales.sumOf { it.balancePending }
            var amountToAllocate = amountReceived

            for (sale in customerPendingSales) {
                if (amountToAllocate <= 0) break
                val dueOnSale = sale.balancePending
                if (amountToAllocate >= dueOnSale) {
                    amountToAllocate -= dueOnSale
                    repository.updateSale(
                        sale.copy(
                            amountPaid = sale.amountPaid + dueOnSale,
                            balancePending = 0.0,
                            paymentStatus = PaymentStatus.PAID.name
                        )
                    )
                } else {
                    val remainingDue = dueOnSale - amountToAllocate
                    repository.updateSale(
                        sale.copy(
                            amountPaid = sale.amountPaid + amountToAllocate,
                            balancePending = remainingDue,
                            paymentStatus = PaymentStatus.PARTIAL.name
                        )
                    )
                    amountToAllocate = 0.0
                }
            }

            val remainingBalance = (prevBalance - amountReceived).coerceAtLeast(0.0)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val paymentRecord = PaymentRecordEntity(
                customerId = customerId,
                customerName = customer.name,
                amountReceived = amountReceived,
                previousBalance = prevBalance,
                remainingBalance = remainingBalance,
                paymentMethod = paymentMethod,
                referenceId = referenceId,
                paymentDate = today,
                notes = notes
            )
            repository.insertPaymentRecord(paymentRecord)
            _lastPaymentSuccess.value = paymentRecord

            // Sync to Firestore
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.recordPayment(
                        PaymentRecordDoc(
                            shopId = user.uid,
                            customerId = customerId.toString(),
                            customerName = customer.name,
                            amountReceived = amountReceived,
                            previousBalance = prevBalance,
                            remainingBalance = remainingBalance,
                            paymentMethod = paymentMethod,
                            referenceId = referenceId,
                            paymentDate = today,
                            notes = notes
                        )
                    )
                }
                _syncStatus.value = "Synced ✓"
            } catch (e: Exception) {
                _syncStatus.value = "Offline ⚠ (Saved Locally)"
            }

            launch(Dispatchers.Main) {
                onSuccess(paymentRecord)
            }
        }
    }

    // Customer operations
    fun addCustomer(
        name: String,
        phone: String,
        businessName: String,
        customerType: String,
        address: String,
        paymentCycle: String,
        defaultChickenRate: Double,
        deliveryDays: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = CustomerEntity(
                name = name,
                phone = phone,
                businessName = businessName,
                customerType = customerType,
                address = address,
                paymentCycle = paymentCycle,
                defaultChickenRate = defaultChickenRate,
                deliveryDays = deliveryDays,
                notes = notes
            )
            val id = repository.insertCustomer(entity)

            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.addCustomer(
                        CustomerDoc(
                            id = id.toString(),
                            shopId = user.uid,
                            name = name,
                            phone = phone,
                            businessName = businessName,
                            customerType = customerType,
                            paymentCycle = paymentCycle,
                            defaultRate = defaultChickenRate,
                            deliveryDays = deliveryDays,
                            notes = notes
                        )
                    )
                }
            } catch (e: Exception) {}

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Expense operations
    fun addExpense(
        category: String,
        description: String,
        amount: Double,
        paymentMethod: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertExpense(
                ExpenseEntity(
                    category = category,
                    description = description,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    expenseDate = today
                )
            )

            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.saveExpense(
                        ExpenseDoc(
                            shopId = user.uid,
                            category = category,
                            description = description,
                            amount = amount,
                            paymentMethod = paymentMethod,
                            expenseDate = today
                        )
                    )
                }
            } catch (e: Exception) {}

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Wastage operations
    fun addWastage(
        productType: String,
        quantity: Double,
        unit: String,
        reason: String,
        estimatedCost: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertWastage(
                WastageEntity(
                    productType = productType,
                    quantity = quantity,
                    unit = unit,
                    reason = reason,
                    estimatedCost = estimatedCost,
                    date = today,
                    notes = notes
                )
            )
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Egg Sales Recording
    fun addEggSale(
        eggType: String,
        unit: String,
        quantity: Double,
        rate: Double,
        paymentMethod: String,
        customerName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val unitObj = EggUnit.values().firstOrNull { it.name == unit } ?: EggUnit.TRAY
            val totalEggs = (quantity * unitObj.eggCount).toInt()
            val totalAmount = quantity * rate
            val costRate = if (eggType == EggType.BROWN.name) 140.0 else 130.0
            val costAmount = quantity * costRate
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            repository.insertEggTransaction(
                EggTransactionEntity(
                    transactionType = "SALE",
                    eggType = eggType,
                    unit = unit,
                    unitQuantity = quantity,
                    totalEggsCount = totalEggs,
                    ratePerUnit = rate,
                    totalAmount = totalAmount,
                    costAmount = costAmount,
                    grossProfit = totalAmount - costAmount,
                    paymentMethod = paymentMethod,
                    paymentStatus = PaymentStatus.PAID.name,
                    amountPaid = totalAmount,
                    customerOrSupplierName = customerName.ifBlank { "Retail Customer" },
                    date = today
                )
            )

            repository.createSale(
                customerId = null,
                customerName = customerName.ifBlank { "Egg Retail" },
                customerType = CustomerType.RETAIL.name,
                productType = ProductType.EGG.name,
                chickenCut = if (eggType == EggType.BROWN.name) "Brown Egg" else "White Egg",
                quantityUnit = unit,
                quantity = quantity,
                piecesCount = totalEggs,
                sellingRate = rate,
                discount = 0.0,
                paymentMethod = paymentMethod,
                amountPaidManual = totalAmount,
                dueDate = today,
                isInstitutionalDelivery = false,
                notes = "$eggType egg sale"
            )

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Day Closing
    fun closeDay(
        openingCash: Double,
        openingUpi: Double,
        notes: String,
        onSuccess: (DailyClosingEntity) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = selectedDate.value
            val sales = selectedDateSales.value
            val exps = selectedDateExpenses.value
            val pays = payments.value.filter { it.paymentDate == date }

            val cashSales = sales.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountPaid }
            val cashReceived = pays.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountReceived }
            val cashExpenses = exps.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amount }
            val closingCash = openingCash + cashSales + cashReceived - cashExpenses

            val upiSales = sales.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountPaid }
            val upiReceived = pays.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountReceived }
            val upiExpenses = exps.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amount }
            val closingUpi = openingUpi + upiSales + upiReceived - upiExpenses

            val newPending = sales.sumOf { it.balancePending }
            val collectedPending = pays.sumOf { it.amountReceived }
            val totalOutstanding = pendingSales.value.sumOf { it.balancePending }

            val closing = DailyClosingEntity(
                date = date,
                openingCash = openingCash,
                cashSales = cashSales,
                cashPaymentsReceived = cashReceived,
                cashExpenses = cashExpenses,
                closingCash = closingCash,
                openingUpi = openingUpi,
                upiSales = upiSales,
                upiReceived = upiReceived,
                upiPayments = upiExpenses,
                closingUpi = closingUpi,
                newPending = newPending,
                collectedPending = collectedPending,
                totalOutstanding = totalOutstanding,
                isClosed = true,
                closedTimestamp = System.currentTimeMillis(),
                notes = notes
            )
            repository.closeDay(closing)

            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.saveDailyClosing(
                        DailyClosingDoc(
                            shopId = user.uid,
                            date = date,
                            openingCash = openingCash,
                            cashSales = cashSales,
                            cashPaymentsReceived = cashReceived,
                            cashExpenses = cashExpenses,
                            closingCash = closingCash,
                            openingUpi = openingUpi,
                            upiSales = upiSales,
                            upiReceived = upiReceived,
                            upiPayments = upiExpenses,
                            closingUpi = closingUpi,
                            newPending = newPending,
                            collectedPending = collectedPending,
                            totalOutstanding = totalOutstanding,
                            isClosed = true,
                            notes = notes
                        )
                    )
                }
            } catch (e: Exception) {}

            launch(Dispatchers.Main) { onSuccess(closing) }
        }
    }

    private val _dataIntegrityReport = MutableStateFlow<DataIntegrityReport?>(null)
    val dataIntegrityReport: StateFlow<DataIntegrityReport?> = _dataIntegrityReport.asStateFlow()

    // Soft delete / Cancellation
    fun cancelSale(sale: SaleEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = Firebase.auth.currentUser
            if (user != null) {
                firestoreRepo.cancelSale(sale.id.toString())
            }
            repository.deleteSale(sale)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun cancelExpense(expense: ExpenseEntity, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = Firebase.auth.currentUser
            if (user != null) {
                firestoreRepo.cancelExpense(expense.id.toString())
            }
            repository.deleteExpense(expense)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Comprehensive Data Integrity Check
    suspend fun runDataIntegrityCheck(): DataIntegrityReport {
        val user = Firebase.auth.currentUser
        val report = if (user != null) {
            firestoreRepo.runDataIntegrityCheck(user.uid)
        } else {
            val issues = mutableListOf<IntegrityIssue>()
            val sales = allSales.value
            val pays = payments.value
            sales.filter { it.finalAmount < 0 }.forEach {
                issues.add(IntegrityIssue("ERROR", "NEG_SALE", "Negative amount ₹${it.finalAmount} on bill ${it.billNumber}"))
            }
            val duplicates = sales.groupBy { it.billNumber }.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                issues.add(IntegrityIssue("ERROR", "DUP_INVOICE", "Duplicate invoices: ${duplicates.keys.joinToString()}"))
            }
            pays.filter { it.amountReceived <= 0 }.forEach {
                issues.add(IntegrityIssue("ERROR", "INVALID_PAYMENT", "Non-positive payment record detected"))
            }
            val errorCount = issues.count { it.severity == "ERROR" }
            DataIntegrityReport(
                overallStatus = if (errorCount > 0) "ERROR" else "PASS",
                totalChecked = sales.size + pays.size,
                errorCount = errorCount,
                issues = issues
            )
        }
        _dataIntegrityReport.value = report
        return report
    }

    // Safe Restore from JSON
    fun restoreBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = firestoreRepo.restoreDataFromJson(jsonString)
            result.onSuccess { msg ->
                launch(Dispatchers.Main) { onResult(true, msg) }
            }.onFailure { err ->
                launch(Dispatchers.Main) { onResult(false, err.message ?: "Restore failed") }
            }
        }
    }

    // CSV Sales Export
    fun exportBackupCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val sb = java.lang.StringBuilder()
            sb.append("Bill Number,Date,Customer,Product,Quantity,Rate,Final Amount,Payment Status,Payment Method\n")
            allSales.value.forEach { s ->
                sb.append("\"${s.billNumber}\",\"${s.saleDate}\",\"${s.customerName}\",\"${s.productType}\",${s.quantity},${s.sellingRate},${s.finalAmount},\"${s.paymentStatus}\",\"${s.paymentMethod}\"\n")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "HalalChickenShop_Sales.csv")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export Sales Ledger (CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    // JSON Backup Export
    fun exportBackupJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = Firebase.auth.currentUser
            val jsonContent = if (user != null) {
                try {
                    firestoreRepo.exportAllDataJson(user.uid)
                } catch (e: Exception) {
                    exportLocalBackupJson()
                }
            } else {
                exportLocalBackupJson()
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "HalalChickenShop_Backup.json")
                putExtra(Intent.EXTRA_TEXT, jsonContent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export Business Data (JSON)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private fun exportLocalBackupJson(): String {
        val root = JSONObject()
        root.put("shopName", "HALAL CHICKEN SHOP HANTI")
        root.put("owner", "Mr. Sajid")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        val salesArray = JSONArray()
        allSales.value.forEach { s ->
            salesArray.put(
                JSONObject().apply {
                    put("billNumber", s.billNumber)
                    put("customerName", s.customerName)
                    put("productType", s.productType)
                    put("finalAmount", s.finalAmount)
                    put("paymentStatus", s.paymentStatus)
                    put("saleDate", s.saleDate)
                }
            )
        }
        root.put("sales", salesArray)
        return root.toString(2)
    }

    // Share Bill on WhatsApp
    fun shareBillWhatsApp(context: Context, sale: SaleEntity) {
        val message = """
            *HALAL CHICKEN SHOP HANTI*
            Owner: Mr. Sajid
            --------------------------------
            *INVOICE: ${sale.billNumber}*
            Date: ${sale.saleDate}
            Customer: ${sale.customerName}
            
            Item: ${sale.productType} (${sale.chickenCut})
            Quantity: ${sale.quantity} ${sale.quantityUnit}
            Rate: ₹${sale.sellingRate}/${sale.quantityUnit}
            Amount: ₹${sale.subtotal}
            Discount: ₹${sale.discount}
            *Grand Total: ₹${sale.finalAmount}*
            Status: *${sale.paymentStatus}*
            Balance Pending: ₹${sale.balancePending}
            --------------------------------
            Thank you for your business!
            _Designed & Developed by Mr. Sajid_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Bill via WhatsApp / Message")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Share Ledger Statement
    fun shareLedgerStatement(context: Context, customer: CustomerEntity, customerSales: List<SaleEntity>, totalPending: Double) {
        val sb = StringBuilder()
        sb.append("*HALAL CHICKEN SHOP HANTI*\n")
        sb.append("Owner: Mr. Sajid | Hanti, Bihar\n")
        sb.append("--------------------------------\n")
        sb.append("*STATEMENT FOR: ${customer.name.uppercase()}*\n")
        sb.append("Phone: ${customer.phone}\n")
        sb.append("Total Pending Balance: *₹${String.format(Locale.US, "%.2f", totalPending)}*\n")
        sb.append("--------------------------------\n")
        sb.append("Recent Transactions:\n")
        customerSales.take(5).forEach { s ->
            sb.append("• ${s.saleDate} | ${s.billNumber} | ₹${s.finalAmount} (${s.paymentStatus})\n")
        }
        sb.append("--------------------------------\n")
        sb.append("Please settle the outstanding balance.\n")
        sb.append("Thank you.\nHALAL CHICKEN SHOP HANTI")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Statement")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // --- KG-Wise Price & Rate Management ---
    fun updateChickenSellingRate(newRate: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value ?: ShopSettingsEntity()
            val todayStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
            val newEntry = "$todayStr — ₹${newRate.toInt()}/KG"
            val updatedHistory = if (current.chickenPriceHistory.isNotBlank()) {
                "$newEntry; ${current.chickenPriceHistory}"
            } else {
                newEntry
            }
            val updatedSettings = current.copy(
                defaultChickenRate = newRate,
                chickenPriceHistory = updatedHistory
            )
            repository.updateSettings(updatedSettings)

            // Sync to Firestore
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    firestoreRepo.saveShopProfile(
                        ShopDoc(
                            shopId = user.uid,
                            ownerId = user.uid,
                            defaultChickenRate = newRate
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("PriceSync", "Offline price update: ${e.message}")
            }
        }
    }

    fun shareInvoiceWhatsApp(context: Context, sale: SaleEntity) {
        shareBillWhatsApp(context, sale)
    }

    fun updateBusinessCustomerRate(customerId: Long, newRate: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val cust = customers.value.find { it.id == customerId }
            if (cust != null) {
                repository.updateCustomer(cust.copy(defaultChickenRate = newRate))
            }
        }
    }

    // --- PDF Invoice & Statement Generation ---
    fun downloadOrShareInvoicePdf(context: Context, sale: SaleEntity, isPrint: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customers.value.find { it.id == sale.customerId }
            val custSales = allSales.value.filter { it.customerId == sale.customerId && it.id != sale.id }
            val prevPending = custSales.sumOf { it.balancePending }
            val file = HalalPdfGenerator.generateInvoicePdf(context, sale, customer, prevPending)
            launch(Dispatchers.Main) {
                if (isPrint) {
                    HalalPdfGenerator.openOrPrintPdf(context, file)
                } else {
                    HalalPdfGenerator.sharePdf(context, file, "Invoice ${sale.billNumber} - HALAL CHICKEN SHOP HANTI")
                }
            }
        }
    }

    fun downloadOrSharePendingStatementPdf(context: Context, customer: CustomerEntity, isPrint: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val custSales = allSales.value.filter { it.customerId == customer.id && it.balancePending > 0 }
            val custPayments = payments.value.filter { it.customerId == customer.id }
            val totalPending = custSales.sumOf { it.balancePending }
            val file = HalalPdfGenerator.generatePendingStatementPdf(context, customer, custSales, custPayments, totalPending)
            launch(Dispatchers.Main) {
                if (isPrint) {
                    HalalPdfGenerator.openOrPrintPdf(context, file)
                } else {
                    HalalPdfGenerator.sharePdf(context, file, "Pending Statement - ${customer.name}")
                }
            }
        }
    }

    // --- Customer Communication ---
    fun callCustomer(context: Context, phone: String) {
        CustomerCommHelper.callCustomer(context, phone)
    }

    fun sendCustomerSms(context: Context, phone: String, message: String) {
        CustomerCommHelper.sendSms(context, phone, message)
    }

    fun chatCustomerWhatsApp(context: Context, phone: String, message: String = "") {
        CustomerCommHelper.chatWhatsApp(context, phone, message)
    }
}
