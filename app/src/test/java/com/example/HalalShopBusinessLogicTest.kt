package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class HalalShopBusinessLogicTest {

    @Test
    fun testLiveChickenAcquisitionAndYieldCalculation() {
        val numberOfBirds = 100
        val liveWeightKg = 180.0
        val purchaseRatePerKg = 160.0
        val transportCost = 800.0
        val otherCost = 400.0

        val avgWeightPerBird = liveWeightKg / numberOfBirds
        assertEquals(1.80, avgWeightPerBird, 0.001)

        val purchaseAmount = liveWeightKg * purchaseRatePerKg
        assertEquals(28800.0, purchaseAmount, 0.001)

        val totalBatchCost = purchaseAmount + transportCost + otherCost
        assertEquals(30000.0, totalBatchCost, 0.001)

        // Processing yield
        val usableChickenWeightKg = 145.0
        val wasteOffalWeightKg = 35.0

        val yieldPercentage = (usableChickenWeightKg / liveWeightKg) * 100
        assertEquals(80.555, yieldPercentage, 0.01)

        val lossPercentage = (wasteOffalWeightKg / liveWeightKg) * 100
        assertEquals(19.444, lossPercentage, 0.01)

        val costPerUsableKg = totalBatchCost / usableChickenWeightKg
        assertEquals(206.896, costPerUsableKg, 0.01)
    }

    @Test
    fun testSmartKgRateCalculation() {
        val quantityKg = 2.750
        val ratePerKg = 220.0
        val amount = quantityKg * ratePerKg
        assertEquals(605.0, amount, 0.001)
    }

    @Test
    fun testEggUnitConversions() {
        val trays = 7
        val eggsPerTray = 30
        val totalEggsFromTrays = trays * eggsPerTray
        assertEquals(210, totalEggsFromTrays)

        val cardboardEggs = 210
        assertEquals(totalEggsFromTrays, cardboardEggs)
    }

    @Test
    fun testPaymentSettlementLogic() {
        val initialSalePending = 8500.0
        val partialPaymentReceived = 5000.0

        val remainingAfterPartial = initialSalePending - partialPaymentReceived
        assertEquals(3500.0, remainingAfterPartial, 0.001)

        val finalPaymentReceived = 3500.0
        val remainingAfterFull = remainingAfterPartial - finalPaymentReceived
        assertEquals(0.0, remainingAfterFull, 0.001)
    }

    @Test
    fun testProfitCalculationWithExpenses() {
        val salesRevenue = 48750.0
        val costOfGoodsSold = 31200.0
        val directExpenses = 1250.0

        val grossProfit = salesRevenue - costOfGoodsSold
        assertEquals(17550.0, grossProfit, 0.001)

        val netProfit = grossProfit - directExpenses
        assertEquals(16300.0, netProfit, 0.001)
    }
}
