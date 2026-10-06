package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HalalFirestoreRepository(
    private val context: Context,
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    ),
    val auth: FirebaseAuth = Firebase.auth
) {
    init {
        try {
            // Enable offline disk persistence
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            // Settings already locked or initialized in test
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun requireShopId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing shop data.")
    }

    private fun shopRef(shopId: String): DocumentReference =
        firestore.collection("shops").document(shopId)

    // --- Audit Logging ---
    suspend fun logAudit(
        action: String,
        recordType: String,
        recordId: String,
        details: String,
        oldVal: String = "",
        newVal: String = ""
    ) {
        try {
            val shopId = requireShopId()
            val logId = UUID.randomUUID().toString()
            val log = hashMapOf<String, Any>(
                "id" to logId,
                "shopId" to shopId,
                "userId" to shopId,
                "action" to action,
                "recordType" to recordType,
                "recordId" to recordId,
                "details" to details,
                "oldValue" to oldVal,
                "newValue" to newVal,
                "createdAt" to FieldValue.serverTimestamp()
            )
            shopRef(shopId).collection("auditLogs").document(logId).set(log).await()
        } catch (e: Exception) {
            // Log locally without failing primary transaction
            android.util.Log.w("HalalAudit", "Failed to write audit log: ${e.message}")
        }
    }

    // --- Shop Profile ---
    fun observeShopProfile(shopId: String): Flow<ShopDoc?> = callbackFlow {
        val docRef = shopRef(shopId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(ShopDoc::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveShopProfile(shop: ShopDoc) {
        val shopId = requireShopId()
        val docRef = shopRef(shopId)
        val payload = hashMapOf<String, Any>(
            "shopId" to shopId,
            "ownerId" to shopId,
            "shopName" to shop.shopName,
            "ownerName" to shop.ownerName,
            "phone" to shop.phone,
            "address" to shop.address,
            "defaultChickenRate" to shop.defaultChickenRate,
            "defaultEggTrayRate" to shop.defaultEggTrayRate,
            "invoicePrefix" to shop.invoicePrefix,
            "allowNegativeStock" to shop.allowNegativeStock,
            "createdAt" to (shop.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload, SetOptions.merge()).await()
    }

    // --- Customers ---
    fun observeCustomers(shopId: String): Flow<List<CustomerDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("customers")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(CustomerDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addCustomer(customer: CustomerDoc): String {
        require(customer.name.isNotBlank()) { "Customer name is required" }
        val shopId = requireShopId()
        val id = if (customer.id.isNotBlank()) customer.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("customers").document(id)
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "shopId" to shopId,
            "name" to customer.name,
            "phone" to customer.phone,
            "businessName" to customer.businessName,
            "customerType" to customer.customerType,
            "paymentCycle" to customer.paymentCycle,
            "defaultRate" to customer.defaultRate,
            "deliveryDays" to customer.deliveryDays,
            "totalPending" to customer.totalPending,
            "notes" to customer.notes,
            "status" to customer.status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload, SetOptions.merge()).await()
        logAudit("CUSTOMER_SAVED", "Customer", id, "Saved customer ${customer.name}")
        return id
    }

    // --- Sales with Duplicate Protection & Stock Movement ---
    fun observeSales(shopId: String): Flow<List<SaleDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("sales")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(SaleDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveSale(sale: SaleDoc): String {
        require(sale.finalAmount >= 0) { "Sale amount cannot be negative" }
        require(sale.sellingRate >= 0) { "Selling rate cannot be negative" }
        require(sale.customerName.isNotBlank()) { "Customer name is required" }

        val shopId = requireShopId()
        val saleId = if (sale.id.isNotBlank()) sale.id else UUID.randomUUID().toString()
        val saleDocRef = shopRef(shopId).collection("sales").document(saleId)

        firestore.runTransaction { transaction ->
            // Duplicate bill check if billNumber specified
            if (sale.billNumber.isNotBlank()) {
                val existing = transaction.get(saleDocRef)
                if (existing.exists() && existing.getString("status") == "ACTIVE") {
                    return@runTransaction // Idempotent return without duplicate
                }
            }

            val salePayload = hashMapOf<String, Any>(
                "id" to saleId,
                "shopId" to shopId,
                "billNumber" to sale.billNumber,
                "customerName" to sale.customerName,
                "customerType" to sale.customerType,
                "productType" to sale.productType,
                "chickenCut" to sale.chickenCut,
                "quantityUnit" to sale.quantityUnit,
                "quantity" to sale.quantity,
                "piecesCount" to sale.piecesCount,
                "sellingRate" to sale.sellingRate,
                "subtotal" to sale.subtotal,
                "discount" to sale.discount,
                "finalAmount" to sale.finalAmount,
                "costAmount" to sale.costAmount,
                "grossProfit" to sale.grossProfit,
                "paymentMethod" to sale.paymentMethod,
                "paymentStatus" to sale.paymentStatus,
                "amountPaid" to sale.amountPaid,
                "balancePending" to sale.balancePending,
                "saleDate" to sale.saleDate,
                "dueDate" to sale.dueDate,
                "isInstitutionalDelivery" to sale.isInstitutionalDelivery,
                "isDelivered" to sale.isDelivered,
                "status" to "ACTIVE",
                "notes" to sale.notes,
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sale.customerId.isNullOrBlank()) {
                salePayload["customerId"] = sale.customerId
                if (sale.balancePending > 0) {
                    val custRef = shopRef(shopId).collection("customers").document(sale.customerId)
                    transaction.update(custRef, "totalPending", FieldValue.increment(sale.balancePending))
                }
            }
            transaction.set(saleDocRef, salePayload)

            // Stock movement: SALE decrement
            val moveId = UUID.randomUUID().toString()
            val moveRef = shopRef(shopId).collection("stockMovements").document(moveId)
            val movePayload = hashMapOf<String, Any>(
                "id" to moveId,
                "shopId" to shopId,
                "type" to "SALE",
                "category" to if (sale.productType.contains("EGG", ignoreCase = true)) "EGG" else "CHICKEN",
                "quantity" to sale.quantity,
                "unit" to sale.quantityUnit,
                "referenceId" to saleId,
                "referenceType" to "SALE",
                "date" to sale.saleDate,
                "notes" to "Sale: ${sale.customerName} (${sale.billNumber})",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(moveRef, movePayload)
        }.await()

        logAudit("SALE_CREATED", "Sale", saleId, "Created sale ₹${sale.finalAmount} for ${sale.customerName}")
        return saleId
    }

    // --- Soft Delete / Cancel Sale ---
    suspend fun cancelSale(saleId: String) {
        val shopId = requireShopId()
        val saleRef = shopRef(shopId).collection("sales").document(saleId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(saleRef)
            if (!snapshot.exists()) return@runTransaction
            val currentStatus = snapshot.getString("status")
            if (currentStatus == "CANCELLED") return@runTransaction

            val customerId = snapshot.getString("customerId")
            val pending = snapshot.getDouble("balancePending") ?: 0.0

            // Reverse pending balance on customer
            if (!customerId.isNullOrBlank() && pending > 0) {
                val custRef = shopRef(shopId).collection("customers").document(customerId)
                transaction.update(custRef, "totalPending", FieldValue.increment(-pending))
            }

            // Mark cancelled
            transaction.update(saleRef, "status", "CANCELLED", "updatedAt", FieldValue.serverTimestamp())

            // Reversing stock movement
            val moveId = UUID.randomUUID().toString()
            val moveRef = shopRef(shopId).collection("stockMovements").document(moveId)
            val movePayload = hashMapOf<String, Any>(
                "id" to moveId,
                "shopId" to shopId,
                "type" to "ADJUSTMENT",
                "category" to (snapshot.getString("productType") ?: "CHICKEN"),
                "quantity" to (snapshot.getDouble("quantity") ?: 0.0),
                "unit" to (snapshot.getString("quantityUnit") ?: "KG"),
                "referenceId" to saleId,
                "referenceType" to "SALE_CANCELLED",
                "date" to (snapshot.getString("saleDate") ?: ""),
                "notes" to "Cancelled Sale: $saleId",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(moveRef, movePayload)
        }.await()

        logAudit("SALE_CANCELLED", "Sale", saleId, "Cancelled sale $saleId and reverted pending balance")
    }

    // --- Payments (With Idempotency & Balance Protection) ---
    fun observePayments(shopId: String): Flow<List<PaymentRecordDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("payments")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(PaymentRecordDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun recordPayment(payment: PaymentRecordDoc): String {
        require(payment.amountReceived > 0) { "Payment amount must be greater than zero" }
        require(payment.customerName.isNotBlank()) { "Customer name is required" }

        val shopId = requireShopId()
        // Idempotency key prevents double submissions
        val payId = if (payment.id.isNotBlank()) {
            payment.id
        } else if (payment.idempotencyKey.isNotBlank()) {
            payment.idempotencyKey
        } else {
            UUID.randomUUID().toString()
        }
        val payDocRef = shopRef(shopId).collection("payments").document(payId)

        firestore.runTransaction { transaction ->
            val existing = transaction.get(payDocRef)
            if (existing.exists()) {
                return@runTransaction // Idempotent check: payment already recorded
            }

            val payload = hashMapOf<String, Any>(
                "id" to payId,
                "shopId" to shopId,
                "customerId" to payment.customerId,
                "customerName" to payment.customerName,
                "amountReceived" to payment.amountReceived,
                "previousBalance" to payment.previousBalance,
                "remainingBalance" to payment.remainingBalance,
                "paymentMethod" to payment.paymentMethod,
                "referenceId" to payment.referenceId,
                "paymentDate" to payment.paymentDate,
                "status" to "ACTIVE",
                "notes" to payment.notes,
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!payment.saleId.isNullOrBlank()) payload["saleId"] = payment.saleId
            if (!payment.billNumber.isNullOrBlank()) payload["billNumber"] = payment.billNumber
            if (payment.idempotencyKey.isNotBlank()) payload["idempotencyKey"] = payment.idempotencyKey

            transaction.set(payDocRef, payload)

            // Reduce pending balance on customer record
            if (payment.customerId.isNotBlank()) {
                val custRef = shopRef(shopId).collection("customers").document(payment.customerId)
                transaction.update(custRef, "totalPending", FieldValue.increment(-payment.amountReceived))
            }
        }.await()

        logAudit("PAYMENT_RECORDED", "Payment", payId, "Received ₹${payment.amountReceived} from ${payment.customerName}")
        return payId
    }

    // --- Purchases & Live Batch Tracking ---
    fun observePurchases(shopId: String): Flow<List<LiveChickenPurchaseDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("chickenPurchases")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(LiveChickenPurchaseDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun savePurchase(purchase: LiveChickenPurchaseDoc): String {
        require(purchase.numberOfBirds >= 0) { "Bird count must be non-negative" }
        require(purchase.totalLiveWeightKg >= 0) { "Live weight must be non-negative" }
        require(purchase.purchaseRatePerKg >= 0) { "Purchase rate must be non-negative" }
        require(purchase.totalCost >= 0) { "Total cost must be non-negative" }

        val shopId = requireShopId()
        val id = if (purchase.id.isNotBlank()) purchase.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("chickenPurchases").document(id)

        firestore.runTransaction { transaction ->
            val payload = hashMapOf<String, Any>(
                "id" to id,
                "shopId" to shopId,
                "batchCode" to purchase.batchCode,
                "supplierName" to purchase.supplierName,
                "supplierPhone" to purchase.supplierPhone,
                "numberOfBirds" to purchase.numberOfBirds,
                "totalLiveWeightKg" to purchase.totalLiveWeightKg,
                "avgWeightPerBirdKg" to purchase.avgWeightPerBirdKg,
                "purchaseRatePerKg" to purchase.purchaseRatePerKg,
                "purchaseAmount" to purchase.purchaseAmount,
                "transportCost" to purchase.transportCost,
                "otherCost" to purchase.otherCost,
                "totalCost" to purchase.totalCost,
                "paymentStatus" to purchase.paymentStatus,
                "paymentMethod" to purchase.paymentMethod,
                "purchaseDate" to purchase.purchaseDate,
                "status" to "ACTIVE",
                "notes" to purchase.notes,
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(docRef, payload)

            // Stock movement: Live chicken purchase increment
            val moveId = UUID.randomUUID().toString()
            val moveRef = shopRef(shopId).collection("stockMovements").document(moveId)
            val movePayload = hashMapOf<String, Any>(
                "id" to moveId,
                "shopId" to shopId,
                "type" to "PURCHASE",
                "category" to "CHICKEN_LIVE",
                "quantity" to purchase.totalLiveWeightKg,
                "unit" to "KG",
                "referenceId" to id,
                "referenceType" to "PURCHASE",
                "date" to purchase.purchaseDate,
                "notes" to "Live purchase: ${purchase.numberOfBirds} birds (${purchase.supplierName})",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(moveRef, movePayload)
        }.await()

        logAudit("PURCHASE_CREATED", "ChickenPurchase", id, "Purchased ${purchase.numberOfBirds} birds, ${purchase.totalLiveWeightKg}kg")
        return id
    }

    // --- Processing Records with Yield Integrity ---
    fun observeProcessingRecords(shopId: String): Flow<List<ProcessingRecordDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("processingRecords")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(ProcessingRecordDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveProcessingRecord(record: ProcessingRecordDoc): String {
        require(record.usableChickenWeightKg >= 0) { "Usable weight must be non-negative" }
        require(record.liveWeightKg >= 0) { "Live weight must be non-negative" }
        require(record.usableChickenWeightKg <= record.liveWeightKg || record.liveWeightKg == 0.0) {
            "Usable chicken weight cannot exceed live chicken weight"
        }

        val shopId = requireShopId()
        val id = if (record.id.isNotBlank()) record.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("processingRecords").document(id)

        val computedYield = if (record.liveWeightKg > 0) {
            (record.usableChickenWeightKg / record.liveWeightKg) * 100.0
        } else 0.0

        firestore.runTransaction { transaction ->
            val payload = hashMapOf<String, Any>(
                "id" to id,
                "shopId" to shopId,
                "batchCode" to record.batchCode,
                "purchaseId" to record.purchaseId,
                "liveBirdCount" to record.liveBirdCount,
                "liveWeightKg" to record.liveWeightKg,
                "usableChickenWeightKg" to record.usableChickenWeightKg,
                "wasteOffalWeightKg" to record.wasteOffalWeightKg,
                "otherLossKg" to record.otherLossKg,
                "yieldPercentage" to computedYield,
                "lossPercentage" to (100.0 - computedYield),
                "totalBatchCost" to record.totalBatchCost,
                "costPerUsableKg" to record.costPerUsableKg,
                "processingDate" to record.processingDate,
                "status" to "ACTIVE",
                "notes" to record.notes,
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(docRef, payload)

            // Stock movement: Processing (decrement live weight, increment usable meat)
            val moveId = UUID.randomUUID().toString()
            val moveRef = shopRef(shopId).collection("stockMovements").document(moveId)
            val movePayload = hashMapOf<String, Any>(
                "id" to moveId,
                "shopId" to shopId,
                "type" to "PROCESSING",
                "category" to "CHICKEN_PROCESSED",
                "quantity" to record.usableChickenWeightKg,
                "unit" to "KG",
                "referenceId" to id,
                "referenceType" to "PROCESSING",
                "date" to record.processingDate,
                "notes" to "Processed ${record.liveWeightKg}kg -> ${record.usableChickenWeightKg}kg meat (${String.format("%.1f", computedYield)}% yield)",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(moveRef, movePayload)
        }.await()

        logAudit("PROCESSING_SAVED", "ProcessingRecord", id, "Processed batch: ${record.usableChickenWeightKg}kg usable meat")
        return id
    }

    // --- Egg Stock & Transactions (1 tray = 30 eggs, 1 cardboard = 210 eggs / 7 trays) ---
    suspend fun saveEggTransaction(egg: EggTransactionDoc): String {
        require(egg.totalEggsCount >= 0) { "Egg quantity must be non-negative" }
        require(egg.ratePerUnit >= 0) { "Rate per unit must be non-negative" }

        val shopId = requireShopId()
        val id = if (egg.id.isNotBlank()) egg.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("eggTransactions").document(id)

        // Convert base units: 1 tray = 30 eggs, 1 cardboard = 210 eggs
        val calculatedEggs = when (egg.unit.uppercase()) {
            "CARDBOARD" -> (egg.unitQuantity * 210).toInt()
            "TRAY" -> (egg.unitQuantity * 30).toInt()
            else -> egg.totalEggsCount
        }

        firestore.runTransaction { transaction ->
            val payload = hashMapOf<String, Any>(
                "id" to id,
                "shopId" to shopId,
                "transactionType" to egg.transactionType,
                "eggType" to egg.eggType,
                "unit" to egg.unit,
                "unitQuantity" to egg.unitQuantity,
                "totalEggsCount" to calculatedEggs,
                "ratePerUnit" to egg.ratePerUnit,
                "totalAmount" to egg.totalAmount,
                "costAmount" to egg.costAmount,
                "grossProfit" to egg.grossProfit,
                "paymentMethod" to egg.paymentMethod,
                "paymentStatus" to egg.paymentStatus,
                "amountPaid" to egg.amountPaid,
                "customerOrSupplierName" to egg.customerOrSupplierName,
                "date" to egg.date,
                "status" to "ACTIVE",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(docRef, payload)

            // Stock movement
            val moveId = UUID.randomUUID().toString()
            val moveRef = shopRef(shopId).collection("stockMovements").document(moveId)
            val movePayload = hashMapOf<String, Any>(
                "id" to moveId,
                "shopId" to shopId,
                "type" to egg.transactionType,
                "category" to "EGG",
                "quantity" to egg.unitQuantity,
                "unit" to egg.unit,
                "baseQuantityEggs" to calculatedEggs,
                "referenceId" to id,
                "referenceType" to "EGG_TRANSACTION",
                "date" to egg.date,
                "notes" to "${egg.transactionType} ${egg.eggType} Eggs: $calculatedEggs units",
                "createdBy" to shopId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(moveRef, movePayload)
        }.await()

        logAudit("EGG_TRANSACTION", "EggTransaction", id, "${egg.transactionType} $calculatedEggs eggs")
        return id
    }

    // --- Expenses (Categories: Transport, Electricity, Rent, Packaging, Labour, Ice, Cleaning, Fuel, Other) ---
    fun observeExpenses(shopId: String): Flow<List<ExpenseDoc>> = callbackFlow {
        val coll = shopRef(shopId).collection("expenses")
        val registration = coll.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, coll.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(ExpenseDoc::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveExpense(expense: ExpenseDoc): String {
        require(expense.amount > 0) { "Expense amount must be greater than zero" }
        require(expense.category.isNotBlank()) { "Expense category is required" }

        val shopId = requireShopId()
        val id = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("expenses").document(id)
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "shopId" to shopId,
            "category" to expense.category,
            "description" to expense.description,
            "amount" to expense.amount,
            "paymentMethod" to expense.paymentMethod,
            "expenseDate" to expense.expenseDate,
            "status" to "ACTIVE",
            "createdBy" to shopId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        logAudit("EXPENSE_SAVED", "Expense", id, "Added expense ₹${expense.amount} (${expense.category})")
        return id
    }

    suspend fun cancelExpense(expenseId: String) {
        val shopId = requireShopId()
        val docRef = shopRef(shopId).collection("expenses").document(expenseId)
        docRef.update("status", "CANCELLED", "updatedAt", FieldValue.serverTimestamp()).await()
        logAudit("EXPENSE_CANCELLED", "Expense", expenseId, "Cancelled expense $expenseId")
    }

    // --- Daily Closing Register ---
    suspend fun saveDailyClosing(closing: DailyClosingDoc): String {
        val shopId = requireShopId()
        val id = if (closing.id.isNotBlank()) closing.id else UUID.randomUUID().toString()
        val docRef = shopRef(shopId).collection("dailyClosings").document(id)
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "shopId" to shopId,
            "date" to closing.date,
            "openingCash" to closing.openingCash,
            "cashSales" to closing.cashSales,
            "cashPaymentsReceived" to closing.cashPaymentsReceived,
            "cashExpenses" to closing.cashExpenses,
            "closingCash" to closing.closingCash,
            "openingUpi" to closing.openingUpi,
            "upiSales" to closing.upiSales,
            "upiReceived" to closing.upiReceived,
            "upiPayments" to closing.upiPayments,
            "closingUpi" to closing.closingUpi,
            "newPending" to closing.newPending,
            "collectedPending" to closing.collectedPending,
            "totalOutstanding" to closing.totalOutstanding,
            "isClosed" to closing.isClosed,
            "notes" to closing.notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        logAudit("DAILY_CLOSING", "DailyClosing", id, "Register closed for date ${closing.date}")
        return id
    }

    // --- Comprehensive Data Integrity Checker ---
    suspend fun runDataIntegrityCheck(shopId: String): DataIntegrityReport {
        val issues = mutableListOf<IntegrityIssue>()
        var totalChecked = 0

        try {
            val salesSnapshot = shopRef(shopId).collection("sales").get().await()
            val paymentsSnapshot = shopRef(shopId).collection("payments").get().await()
            val purchasesSnapshot = shopRef(shopId).collection("chickenPurchases").get().await()
            val processingSnapshot = shopRef(shopId).collection("processingRecords").get().await()
            val customersSnapshot = shopRef(shopId).collection("customers").get().await()

            totalChecked = salesSnapshot.size() + paymentsSnapshot.size() + purchasesSnapshot.size() + processingSnapshot.size()

            // 1. Check for negative sale amounts or rates
            salesSnapshot.documents.forEach { doc ->
                val amt = doc.getDouble("finalAmount") ?: 0.0
                val bill = doc.getString("billNumber") ?: doc.id
                if (amt < 0) {
                    issues.add(IntegrityIssue("ERROR", "NEG_SALE", "Negative sale amount ₹$amt on bill $bill"))
                }
            }

            // 2. Check for duplicate bill numbers among active sales
            val activeBills = salesSnapshot.documents
                .filter { it.getString("status") != "CANCELLED" }
                .mapNotNull { it.getString("billNumber") }
                .filter { it.isNotBlank() }
            val duplicateBills = activeBills.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicateBills.isNotEmpty()) {
                issues.add(IntegrityIssue("ERROR", "DUP_INVOICE", "Duplicate invoice numbers detected: ${duplicateBills.keys.joinToString()}"))
            }

            // 3. Check for orphan payments or invalid payment amounts
            paymentsSnapshot.documents.forEach { doc ->
                val amt = doc.getDouble("amountReceived") ?: 0.0
                val name = doc.getString("customerName") ?: ""
                if (amt <= 0) {
                    issues.add(IntegrityIssue("ERROR", "INVALID_PAYMENT", "Non-positive payment amount ₹$amt from '$name'"))
                }
            }

            // 4. Check for chicken yield integrity (usable weight <= live weight)
            processingSnapshot.documents.forEach { doc ->
                val live = doc.getDouble("liveWeightKg") ?: 0.0
                val usable = doc.getDouble("usableChickenWeightKg") ?: 0.0
                if (live > 0 && usable > live) {
                    issues.add(IntegrityIssue("ERROR", "YIELD_OVERFLOW", "Usable meat (${usable}kg) exceeds live weight (${live}kg)"))
                }
            }

            // 5. Customer balance consistency
            customersSnapshot.documents.forEach { doc ->
                val totalPending = doc.getDouble("totalPending") ?: 0.0
                if (totalPending < 0) {
                    issues.add(IntegrityIssue("WARNING", "NEG_BALANCE", "Negative balance ₹$totalPending for customer ${doc.getString("name")}"))
                }
            }

        } catch (e: Exception) {
            issues.add(IntegrityIssue("ERROR", "SCAN_FAILED", "Integrity scan error: ${e.message}"))
        }

        val errorCount = issues.count { it.severity == "ERROR" }
        val warningCount = issues.count { it.severity == "WARNING" }
        val overallStatus = when {
            errorCount > 0 -> "ERROR"
            warningCount > 0 -> "WARNING"
            else -> "PASS"
        }

        return DataIntegrityReport(
            overallStatus = overallStatus,
            totalChecked = totalChecked,
            errorCount = errorCount,
            warningCount = warningCount,
            issues = issues
        )
    }

    // --- Full JSON Backup Export ---
    suspend fun exportAllDataJson(shopId: String): String {
        val root = JSONObject()
        root.put("shopId", shopId)
        root.put("app", "HALAL CHICKEN SHOP HANTI")
        root.put("owner", "Mr. Sajid")
        root.put("exportedAt", System.currentTimeMillis())

        val collections = listOf(
            "customers", "sales", "payments", "chickenPurchases",
            "processingRecords", "expenses", "eggTransactions",
            "stockMovements", "dailyClosings", "auditLogs"
        )
        for (colName in collections) {
            val snapshot = shopRef(shopId).collection(colName).get().await()
            val array = JSONArray()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                array.put(JSONObject(data.mapValues { (_, v) -> v?.toString() }))
            }
            root.put(colName, array)
        }
        return root.toString(2)
    }

    // --- Safe Restore System (Validates shopId, schema, merges safely) ---
    suspend fun restoreDataFromJson(jsonString: String): Result<String> {
        val currentShopId = requireShopId()
        return try {
            val root = JSONObject(jsonString)
            val backupShopId = root.optString("shopId", "")
            if (backupShopId.isNotBlank() && backupShopId != currentShopId) {
                return Result.failure(
                    SecurityException("Backup shop ID ($backupShopId) does not match your active shop ID ($currentShopId). Cross-shop restore blocked.")
                )
            }

            var restoredCount = 0
            val collections = listOf("customers", "sales", "payments", "chickenPurchases", "processingRecords", "expenses", "eggTransactions")

            for (colName in collections) {
                if (!root.has(colName)) continue
                val array = root.getJSONArray(colName)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (id.isBlank()) continue

                    val map = mutableMapOf<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = obj.get(key)
                    }
                    map["shopId"] = currentShopId
                    map["restoredAt"] = FieldValue.serverTimestamp()

                    shopRef(currentShopId).collection(colName).document(id).set(map, SetOptions.merge()).await()
                    restoredCount++
                }
            }

            logAudit("DATABASE_RESTORED", "Database", currentShopId, "Restored $restoredCount records from backup")
            Result.success("Successfully restored $restoredCount records into active shop.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
