package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HalalShopDao {

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Live Chicken Purchases
    @Query("SELECT * FROM live_chicken_purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<LiveChickenPurchaseEntity>>

    @Query("SELECT * FROM live_chicken_purchases WHERE batchCode = :batchCode LIMIT 1")
    suspend fun getPurchaseByBatchCode(batchCode: String): LiveChickenPurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: LiveChickenPurchaseEntity): Long

    // Processing Records
    @Query("SELECT * FROM processing_records ORDER BY timestamp DESC")
    fun getAllProcessingRecords(): Flow<List<ProcessingRecordEntity>>

    @Query("SELECT * FROM processing_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestProcessingRecord(): Flow<ProcessingRecordEntity?>

    @Query("SELECT * FROM processing_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestProcessingRecordDirect(): ProcessingRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessingRecord(record: ProcessingRecordEntity): Long

    // Sales
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE saleDate = :date ORDER BY timestamp DESC")
    fun getSalesByDate(date: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE paymentStatus IN ('PENDING', 'PARTIAL', 'OVERDUE') ORDER BY timestamp DESC")
    fun getPendingSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE billNumber = :billNumber LIMIT 1")
    suspend fun getSaleByBillNumber(billNumber: String): SaleEntity?

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getSaleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    // Demo Data Cleanup Queries
    @Query("DELETE FROM sales WHERE billNumber LIKE 'HCSH-2026-%'")
    suspend fun deleteDemoSales()

    @Query("DELETE FROM live_chicken_purchases WHERE batchCode = 'LB-2026-001'")
    suspend fun deleteDemoPurchases()

    @Query("DELETE FROM processing_records WHERE batchCode = 'LB-2026-001'")
    suspend fun deleteDemoProcessing()

    @Query("DELETE FROM customers WHERE name IN ('ABC Hotel', 'Rahul Kumar', 'Sharma Restaurant', 'Maa Durga Hotel')")
    suspend fun deleteDemoCustomers()

    @Query("DELETE FROM expenses WHERE description LIKE '%demo%' OR description LIKE '%Initial%'")
    suspend fun deleteDemoExpenses()

    // Egg Transactions
    @Query("SELECT * FROM egg_transactions ORDER BY timestamp DESC")
    fun getAllEggTransactions(): Flow<List<EggTransactionEntity>>

    @Query("SELECT * FROM egg_transactions WHERE date = :date ORDER BY timestamp DESC")
    fun getEggTransactionsByDate(date: String): Flow<List<EggTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEggTransaction(transaction: EggTransactionEntity): Long

    // Payments
    @Query("SELECT * FROM payment_records ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecordEntity): Long

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE expenseDate = :date ORDER BY timestamp DESC")
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Wastage
    @Query("SELECT * FROM wastage_records ORDER BY timestamp DESC")
    fun getAllWastage(): Flow<List<WastageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWastage(wastage: WastageEntity): Long

    // Daily Closings
    @Query("SELECT * FROM daily_closings ORDER BY date DESC")
    fun getAllClosings(): Flow<List<DailyClosingEntity>>

    @Query("SELECT * FROM daily_closings WHERE date = :date LIMIT 1")
    suspend fun getClosingForDate(date: String): DailyClosingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosing(closing: DailyClosingEntity): Long

    @Update
    suspend fun updateClosing(closing: DailyClosingEntity)

    // Shop Settings
    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): ShopSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: ShopSettingsEntity)
}
