package com.example.data

enum class WeatherCondition(val label: String, val hasRainSurge: Boolean, val iconRes: String) {
    CLEAR("Soleado / Normal", false, "☀️"),
    RAIN("Lluvia (+15% Clima Director)", true, "🌧️"),
    STORM("Tormenta (+15% Clima Director)", true, "⛈️")
}

enum class DemandLevel(val label: String, val ratePerKm: Double, val platformCommissionPercent: Int) {
    LOW_DEMAND("Baja Demanda ($8 MXN/km | 15% Com. YaVa!)", 8.0, 15),
    HIGH_DEMAND("Alta Demanda / Hora Pico ($9 MXN/km | 20% Com. YaVa!)", 9.0, 20)
}

data class QuoteResult(
    val distanceKm: Double,
    val ratePerKm: Double,
    val basePriceMxn: Double,
    val finalPriceMxn: Double,
    val platformCommissionRate: Double,
    val platformCommissionAmountMxn: Double,
    val driverEarningsMxn: Double,
    val priceTierLabel: String,
    val isCustomQuote: Boolean,
    val estimatedTimeMinutes: Int,
    val isHighDemand: Boolean,
    val isDirectorWeatherSurgeActive: Boolean,
    val surgeAmountMxn: Double
)

object PricingCalculator {

    fun calculateQuote(
        distanceKm: Double,
        isHighDemand: Boolean = false,
        isDirectorWeatherSurgeActive: Boolean = false
    ): QuoteResult {
        val normalizedDistance = (Math.round(distanceKm * 10.0) / 10.0).coerceAtLeast(1.0)

        val demandLevel = if (isHighDemand) DemandLevel.HIGH_DEMAND else DemandLevel.LOW_DEMAND
        val ratePerKm = demandLevel.ratePerKm
        val commissionPercent = demandLevel.platformCommissionPercent
        val commissionRate = commissionPercent / 100.0

        // Base price = distance * ratePerKm (minimum $35 MXN base)
        val rawBasePrice = (normalizedDistance * ratePerKm).coerceAtLeast(35.0)

        // Director Manual Weather Surge (+15% automatic increment if active)
        val weatherMultiplier = if (isDirectorWeatherSurgeActive) 1.15 else 1.0

        val rawFinalPrice = rawBasePrice * weatherMultiplier
        val finalPriceMxn = Math.round(rawFinalPrice * 100.0) / 100.0
        val surgeAmountMxn = Math.round((finalPriceMxn - rawBasePrice) * 100.0) / 100.0

        val platformCommissionAmount = Math.round((finalPriceMxn * commissionRate) * 100.0) / 100.0
        val driverEarnings = Math.round((finalPriceMxn - platformCommissionAmount) * 100.0) / 100.0

        val isCustom = normalizedDistance > 25.0
        val baseEstTime = (normalizedDistance * 3.2 + 8.0).toInt().coerceAtLeast(10)
        val finalEstTime = if (isDirectorWeatherSurgeActive || isHighDemand) {
            (baseEstTime * 1.25).toInt()
        } else {
            baseEstTime
        }

        val tierLabel = if (isHighDemand) {
            "Alta Demanda ($9/km)"
        } else {
            "Baja Demanda ($8/km)"
        }

        return QuoteResult(
            distanceKm = normalizedDistance,
            ratePerKm = ratePerKm,
            basePriceMxn = rawBasePrice,
            finalPriceMxn = finalPriceMxn,
            platformCommissionRate = commissionRate,
            platformCommissionAmountMxn = platformCommissionAmount,
            driverEarningsMxn = driverEarnings,
            priceTierLabel = tierLabel,
            isCustomQuote = isCustom,
            estimatedTimeMinutes = finalEstTime,
            isHighDemand = isHighDemand,
            isDirectorWeatherSurgeActive = isDirectorWeatherSurgeActive,
            surgeAmountMxn = surgeAmountMxn
        )
    }
}
