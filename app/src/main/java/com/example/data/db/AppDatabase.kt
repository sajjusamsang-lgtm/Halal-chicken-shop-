package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.HalalShopDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        LiveChickenPurchaseEntity::class,
        ProcessingRecordEntity::class,
        SaleEntity::class,
        EggTransactionEntity::class,
        PaymentRecordEntity::class,
        ExpenseEntity::class,
        WastageEntity::class,
        DailyClosingEntity::class,
        ShopSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun halalShopDao(): HalalShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "halal_chicken_shop_hanti.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.halalShopDao())
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.halalShopDao()
                    dao.deleteDemoSales()
                    dao.deleteDemoPurchases()
                    dao.deleteDemoProcessing()
                    dao.deleteDemoCustomers()
                    dao.deleteDemoExpenses()
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: HalalShopDao) {
    val now = System.currentTimeMillis()

    // 1. Initial Settings with owner PIN 9708099035 and default Chicken Rate ₹300/KG
    dao.insertSettings(
        ShopSettingsEntity(
            id = 1,
            shopName = "HALAL CHICKEN SHOP HANTI",
            ownerName = "Mr. Sajid",
            phone = "+91 9708099035",
            address = "Hanti, Bihar, India",
            invoicePrefix = "HCS",
            defaultChickenRate = 300.0,
            defaultEggTrayRateBrown = 180.0,
            defaultEggTrayRateWhite = 170.0,
            biometricEnabled = true,
            ownerPin = "9708099035",
            isDarkMode = true,
            chickenPriceHistory = "01 Oct — ₹300/KG"
        )
    )

    // 2. Pre-configured Business Customer: Delhi Public School Biraul (Zero fake transactions)
    dao.insertCustomer(
        CustomerEntity(
            name = "Delhi Public School Biraul",
            phone = "+91 9708099035",
            businessName = "DPS Biraul",
            customerType = CustomerType.SCHOOL.name,
            address = "Biraul, Bihar",
            paymentCycle = PaymentCycle.MONTHLY.name,
            defaultChickenRate = 290.0,
            deliveryDays = "Sunday, Wednesday",
            notes = "Institutional client. Delivery days: Sunday, Wednesday. Special Rate: ₹290/KG.",
            createdAt = now
        )
    )
}
