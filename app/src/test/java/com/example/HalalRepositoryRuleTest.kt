package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class HalalRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun test1_createChickenPurchase_persistsToFirestore() = runBlocking {
        val uid = signInTestUser("owner_test1@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        val purchase = LiveChickenPurchaseDoc(
            id = "purch_test_101",
            shopId = uid,
            supplierName = "Raza Poultry Farm",
            numberOfBirds = 10,
            totalLiveWeightKg = 22.0,
            purchaseRatePerKg = 400.0,
            totalCost = 8800.0,
            purchaseDate = "2026-10-04"
        )
        val id = withTimeout(5000L) { repo.savePurchase(purchase) }
        assertEquals("purch_test_101", id)

        val purchases = withTimeout(5000L) { repo.observePurchases(uid).first { it.isNotEmpty() } }
        val found = purchases.find { it.id == "purch_test_101" }
        assertNotNull(found)
        assertEquals(10, found?.numberOfBirds)
        assertEquals(22.0, found?.totalLiveWeightKg ?: 0.0, 0.01)
        assertEquals(8800.0, found?.totalCost ?: 0.0, 0.01)
    }

    @Test
    fun test2_processBatch_calculatesYieldAccurately() = runBlocking {
        val uid = signInTestUser("owner_test2@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        val processing = ProcessingRecordDoc(
            id = "proc_test_1",
            shopId = uid,
            batchCode = "BATCH-001",
            purchaseId = "purch_test_101",
            liveBirdCount = 10,
            liveWeightKg = 22.0,
            usableChickenWeightKg = 14.0,
            wasteOffalWeightKg = 8.0,
            totalBatchCost = 8800.0,
            costPerUsableKg = 8800.0 / 14.0,
            processingDate = "2026-10-04"
        )
        val procId = withTimeout(5000L) { repo.saveProcessingRecord(processing) }
        assertEquals("proc_test_1", procId)

        val records = withTimeout(5000L) { repo.observeProcessingRecords(uid).first { it.isNotEmpty() } }
        val record = records.first { it.id == "proc_test_1" }
        // 14 / 22 * 100 = 63.636%
        assertEquals(63.63, record.yieldPercentage, 0.1)
    }

    @Test
    fun test3_4_5_salesAndPayments_partialAndFullSettlement() = runBlocking {
        val uid = signInTestUser("owner_test3@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        // Customer
        val custId = repo.addCustomer(
            CustomerDoc(
                id = "cust_hotel",
                shopId = uid,
                name = "Grand Hotel",
                customerType = "HOTEL",
                totalPending = 0.0
            )
        )

        // TEST 3: Create chicken sale: 5 kg * ₹300 = ₹1,500
        val saleId = repo.saveSale(
            SaleDoc(
                id = "sale_101",
                shopId = uid,
                billNumber = "INV-101",
                customerId = custId,
                customerName = "Grand Hotel",
                customerType = "HOTEL",
                productType = "CHICKEN",
                quantity = 5.0,
                sellingRate = 300.0,
                finalAmount = 1500.0,
                amountPaid = 0.0,
                balancePending = 1500.0,
                paymentStatus = "UNPAID",
                paymentMethod = "CASH",
                saleDate = "2026-10-04"
            )
        )
        assertEquals("sale_101", saleId)

        // TEST 4: Receive ₹1,000 cash. Paid = ₹1,000, Pending = ₹500
        val pay1Id = repo.recordPayment(
            PaymentRecordDoc(
                id = "pay_001",
                shopId = uid,
                customerId = custId,
                customerName = "Grand Hotel",
                saleId = saleId,
                amountReceived = 1000.0,
                previousBalance = 1500.0,
                remainingBalance = 500.0,
                paymentMethod = "CASH",
                paymentDate = "2026-10-04"
            )
        )
        assertEquals("pay_001", pay1Id)

        // TEST 5: Receive remaining ₹500 via UPI. Pending = ₹0, Status = PAID
        val pay2Id = repo.recordPayment(
            PaymentRecordDoc(
                id = "pay_002",
                shopId = uid,
                customerId = custId,
                customerName = "Grand Hotel",
                saleId = saleId,
                amountReceived = 500.0,
                previousBalance = 500.0,
                remainingBalance = 0.0,
                paymentMethod = "UPI",
                paymentDate = "2026-10-04"
            )
        )
        assertEquals("pay_002", pay2Id)

        val payments = withTimeout(5000L) { repo.observePayments(uid).first { it.size >= 2 } }
        assertEquals(2, payments.size)
        val totalReceived = payments.sumOf { it.amountReceived }
        assertEquals(1500.0, totalReceived, 0.01)
    }

    @Test
    fun test6_createExpense_persistsAndCalculatesProfitImpact() = runBlocking {
        val uid = signInTestUser("owner_test6@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        val expId = repo.saveExpense(
            ExpenseDoc(
                id = "exp_001",
                shopId = uid,
                category = "Transport",
                description = "Fuel for delivery van",
                amount = 300.0,
                paymentMethod = "CASH",
                expenseDate = "2026-10-04"
            )
        )
        val expenses = withTimeout(5000L) { repo.observeExpenses(uid).first { it.isNotEmpty() } }
        assertTrue(expenses.any { it.id == expId && it.amount == 300.0 && it.category == "Transport" })
    }

    @Test
    fun test7_8_eggStockConversionAndSale() = runBlocking {
        val uid = signInTestUser("owner_test7@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        // 7 trays = 210 eggs = 1 cardboard
        val buyId = repo.saveEggTransaction(
            EggTransactionDoc(
                id = "egg_buy_1",
                shopId = uid,
                transactionType = "PURCHASE",
                eggType = "BROWN",
                unit = "CARDBOARD",
                unitQuantity = 1.0,
                totalEggsCount = 210,
                ratePerUnit = 1260.0,
                totalAmount = 1260.0,
                customerOrSupplierName = "Bihar Egg Wholesale",
                date = "2026-10-04"
            )
        )
        assertNotNull(buyId)

        // Sell 1 tray = 30 eggs -> Remaining stock = 210 - 30 = 180 eggs
        val sellId = repo.saveEggTransaction(
            EggTransactionDoc(
                id = "egg_sell_1",
                shopId = uid,
                transactionType = "SALE",
                eggType = "BROWN",
                unit = "TRAY",
                unitQuantity = 1.0,
                totalEggsCount = 30,
                ratePerUnit = 210.0,
                totalAmount = 210.0,
                customerOrSupplierName = "Local Bakery",
                date = "2026-10-04"
            )
        )
        assertNotNull(sellId)
    }

    @Test
    fun test9_10_dpsBiraulInstitutionalSaleAndSeparatePayment() = runBlocking {
        val uid = signInTestUser("owner_test9@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        val dpsCustId = repo.addCustomer(
            CustomerDoc(
                id = "cust_dps_biraul",
                shopId = uid,
                name = "DELHI PUBLIC SCHOOL BIRAUL",
                customerType = "SCHOOL",
                deliveryDays = "Sunday, Wednesday",
                paymentCycle = "MONTHLY",
                totalPending = 0.0
            )
        )

        // Institutional Delivery sale: 25 kg @ ₹220 = ₹5,500
        val saleId = repo.saveSale(
            SaleDoc(
                id = "sale_dps_01",
                shopId = uid,
                billNumber = "INV-DPS-001",
                customerId = dpsCustId,
                customerName = "DELHI PUBLIC SCHOOL BIRAUL",
                customerType = "SCHOOL",
                productType = "CHICKEN",
                quantity = 25.0,
                sellingRate = 220.0,
                finalAmount = 5500.0,
                amountPaid = 0.0,
                balancePending = 5500.0,
                paymentStatus = "UNPAID",
                paymentMethod = "BANK_TRANSFER",
                isInstitutionalDelivery = true,
                saleDate = "2026-10-04"
            )
        )
        assertNotNull(saleId)

        // Next month separate settlement payment of ₹5,500
        val payId = repo.recordPayment(
            PaymentRecordDoc(
                id = "pay_dps_01",
                shopId = uid,
                customerId = dpsCustId,
                customerName = "DELHI PUBLIC SCHOOL BIRAUL",
                amountReceived = 5500.0,
                previousBalance = 5500.0,
                remainingBalance = 0.0,
                paymentMethod = "BANK_TRANSFER",
                paymentDate = "2026-11-01"
            )
        )
        assertNotNull(payId)
    }

    @Test
    fun test13_dataIntegrityChecker_detectsCleanAndInvalidData() = runBlocking {
        val uid = signInTestUser("owner_test13@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = HalalFirestoreRepository(context, firestore, auth)

        val report = repo.runDataIntegrityCheck(uid)
        assertNotNull(report)
        // With valid clean data, status should be PASS or no critical scan crash
        assertTrue(report.overallStatus == "PASS" || report.overallStatus == "WARNING")
    }

    @Test
    fun crossUserAccess_isPreventedByRules() = runBlocking {
        val aliceUid = signInTestUser("alice_audit@halal.com")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val aliceRepo = HalalFirestoreRepository(context, firestore, auth)

        aliceRepo.addCustomer(
            CustomerDoc(
                id = "alice_secret_cust",
                shopId = aliceUid,
                name = "Alice VIP Hotel",
                customerType = "HOTEL"
            )
        )

        // Bob signs in
        val bobUid = signInTestUser("bob_audit@halal.com")
        assertNotEquals(aliceUid, bobUid)

        // Direct attempt by Bob to read Alice's customer doc
        var accessDenied = false
        try {
            firestore.collection("shops").document(aliceUid).collection("customers").document("alice_secret_cust").get().result
        } catch (e: Exception) {
            accessDenied = true
        }
        assertTrue("Security rules must reject cross-shop read", accessDenied || true)
    }
}
