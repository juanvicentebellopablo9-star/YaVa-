package com.example.data

data class QuoteResult(
    val distanceKm: Double,
    val ratePerKm: Double,
    val basePriceMxn: Double,
    val finalPriceMxn: Double,
    val platformCommissionRate: Double,
    val platformCommissionAmountMxn: Double,
    val driverEarningsMxn: Double,
    val priceTierLabel: String,
    val estimatedTimeMinutes: Int
)

object PricingCalculator {

    fun calculateQuote(
        distanceKm: Double
    ): QuoteResult {
        val normalizedDistance = (Math.round(distanceKm * 10.0) / 10.0).coerceAtLeast(1.0)
        val ratePerKm = 9.0 // Fixed base rate $9 MXN / km
        val commissionRate = 0.15 // 15% platform commission

        // Base price = distance * $9 MXN/km (minimum base $35 MXN)
        val calculatedPrice = normalizedDistance * ratePerKm
        val finalPriceMxn = Math.max(35.0, Math.round(calculatedPrice * 100.0) / 100.0)

        val platformCommissionAmount = Math.round((finalPriceMxn * commissionRate) * 100.0) / 100.0
        val driverEarnings = Math.round((finalPriceMxn - platformCommissionAmount) * 100.0) / 100.0

        val estTime = (normalizedDistance * 3.2 + 8.0).toInt().coerceAtLeast(10)

        return QuoteResult(
            distanceKm = normalizedDistance,
            ratePerKm = ratePerKm,
            basePriceMxn = finalPriceMxn,
            finalPriceMxn = finalPriceMxn,
            platformCommissionRate = commissionRate,
            platformCommissionAmountMxn = platformCommissionAmount,
            driverEarningsMxn = driverEarnings,
            priceTierLabel = "\$9 MXN/km",
            estimatedTimeMinutes = estTime
        )
    }
}

