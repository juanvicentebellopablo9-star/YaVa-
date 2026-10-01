package com.example.data

/**
 * YaVa! Dynamic Pricing Engine Architecture
 *
 * Implements a multi-variable dynamic fare calculation inspired by modern logistics platforms (Uber / DiDi model)
 * adapted specifically for YaVa! Logistics in Mexico.
 *
 * Formula:
 *   FINAL FARE = [ Base Fare + (Distance * Rate/Km) + (Time * Rate/Min) + Surcharges ] * Dynamic Multiplier
 *   YAva! Commission = Final Fare * PLATFORM_COMMISSION_RATE (15%)
 *   Driver Net Earnings = Final Fare - YAva! Commission (85%)
 */
data class QuoteResult(
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val baseFareMxn: Double,
    val distanceFareMxn: Double,
    val timeFareMxn: Double,
    val surchargesMxn: Double,
    val surgeMultiplier: Double,
    val subtotalMxn: Double,
    val finalPriceMxn: Double,
    val platformCommissionRate: Double,
    val platformCommissionAmountMxn: Double,
    val driverEarningsMxn: Double,
    val priceTierLabel: String,
    val breakdownDescription: String
)

object PricingCalculator {

    // Centralized Platform Commission Configuration (15% as mandated by YaVa! business model)
    const val PLATFORM_COMMISSION_RATE: Double = 0.15

    // Base fare component (pickup & dispatch foundation)
    const val BASE_PICKUP_FARE_MXN: Double = 22.0

    // Distance component: standard rate per kilometer
    const val RATE_PER_KM_MXN: Double = 6.50

    // Time component: rate per estimated transit minute (traffic / urban delay compensation)
    const val RATE_PER_MINUTE_MXN: Double = 1.20

    // Absolute minimum operational fare for any delivery
    const val MINIMUM_OPERATIONAL_FARE_MXN: Double = 35.0

    /**
     * Calculates the dynamic price quote considering distance, traffic time, package weight,
     * weather conditions, and real-time demand surge multiplier.
     */
    fun calculateQuote(
        distanceKm: Double,
        estimatedMinutes: Int? = null,
        packageType: String = "Paquete Pequeño (< 3 kg)",
        weightKg: Double = 1.5,
        isHighDemand: Boolean = false,
        isWeatherSurge: Boolean = false,
        customSurgeMultiplier: Double = 1.0
    ): QuoteResult {
        val normalizedDistance = (Math.round(distanceKm * 10.0) / 10.0).coerceAtLeast(0.5)

        // Estimated transit time in minutes: ~2.8 min per km in urban traffic + 6 min base pickup/handoff
        val calcMinutes = estimatedMinutes ?: ((normalizedDistance * 2.8) + 6.0).toInt().coerceAtLeast(8)

        // Component 1: Base Fare
        val baseFare = BASE_PICKUP_FARE_MXN

        // Component 2: Distance Component
        val distanceFare = Math.round((normalizedDistance * RATE_PER_KM_MXN) * 100.0) / 100.0

        // Component 3: Time Component
        val timeFare = Math.round((calcMinutes * RATE_PER_MINUTE_MXN) * 100.0) / 100.0

        // Component 4: Surcharges (Weight & Weather)
        var surcharges = 0.0
        if (weightKg > 10.0 || packageType.contains("Pesado", ignoreCase = true)) {
            surcharges += 15.0 // Heavy cargo handling fee
        } else if (weightKg > 3.0 || packageType.contains("Mediano", ignoreCase = true)) {
            surcharges += 8.0 // Medium cargo handling fee
        }

        if (isWeatherSurge) {
            surcharges += 12.0 // Bad weather / rain surcharge
        }

        // Component 5: Dynamic Surge Multiplier (Demand & Traffic)
        var multiplier = customSurgeMultiplier.coerceAtLeast(1.0)
        if (isHighDemand && multiplier <= 1.0) {
            multiplier = 1.25 // 25% surge during peak demand hours
        }

        // Calculate Subtotal before multiplier
        val subtotal = baseFare + distanceFare + timeFare + surcharges
        val subtotalGuaranteed = Math.max(MINIMUM_OPERATIONAL_FARE_MXN, subtotal)

        // Calculate Final Client Total
        val finalPriceMxn = Math.round((subtotalGuaranteed * multiplier) * 100.0) / 100.0

        // Financial Breakdown
        val platformCommissionAmount = Math.round((finalPriceMxn * PLATFORM_COMMISSION_RATE) * 100.0) / 100.0
        val driverEarnings = Math.round((finalPriceMxn - platformCommissionAmount) * 100.0) / 100.0

        val tierLabel = if (multiplier > 1.0) {
            "Tarifa Dinámica (${String.format(java.util.Locale.US, "%.2fx", multiplier)})"
        } else {
            "Tarifa Estándar Dinámica"
        }

        val breakdown = "Base \$${baseFare} + Distancia (${normalizedDistance} km) \$${distanceFare} + Tiempo (${calcMinutes} min) \$${timeFare}" +
                (if (surcharges > 0) " + Recargos \$${surcharges}" else "") +
                (if (multiplier > 1.0) " × ${multiplier}x" else "")

        return QuoteResult(
            distanceKm = normalizedDistance,
            estimatedTimeMinutes = calcMinutes,
            baseFareMxn = baseFare,
            distanceFareMxn = distanceFare,
            timeFareMxn = timeFare,
            surchargesMxn = surcharges,
            surgeMultiplier = multiplier,
            subtotalMxn = subtotalGuaranteed,
            finalPriceMxn = finalPriceMxn,
            platformCommissionRate = PLATFORM_COMMISSION_RATE,
            platformCommissionAmountMxn = platformCommissionAmount,
            driverEarningsMxn = driverEarnings,
            priceTierLabel = tierLabel,
            breakdownDescription = breakdown
        )
    }
}
