package com.example.ai

import com.example.data.DriverEntity
import com.example.data.OrderEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * ARA System Lucid - Intelligence Engine Module
 * Formatted for operational logistics monitoring.
 * Rule strictly enforced: Never invents fake information. All metrics and suggestions
 * are calculated directly from active, real persisted database records.
 */
object AraSystemLucid {

    data class AnalyticsReport(
        val totalOrders: Int,
        val completedOrders: Int,
        val pendingOrders: Int,
        val inTransitOrders: Int,
        val cancelledOrders: Int,
        val totalRevenueMxn: Double,
        val formattedRevenue: String,
        val averageOrderPriceMxn: Double,
        val completionRatePercent: Int,
        val topPackageType: String
    )

    data class FinancialLedger(
        val cashCollectedMxn: Double,
        val terminalCollectedMxn: Double,
        val speiCollectedMxn: Double,
        val pendingSpeiCount: Int,
        val pendingSpeiAmountMxn: Double,
        val pendingDriverCollectionsCount: Int,
        val pendingDriverCollectionsMxn: Double,
        val confirmedPaymentsCount: Int,
        val totalRevenueConfirmedMxn: Double
    )

    data class OperationsReport(
        val totalDrivers: Int,
        val activeApprovedDrivers: Int,
        val pendingApprovalDrivers: Int,
        val zoneDriverDistribution: Map<String, Int>,
        val highDemandZones: List<String>,
        val averageDeliveriesPerDriver: Float
    )

    data class RecommendationItem(
        val title: String,
        val description: String,
        val priority: PriorityLevel,
        val category: String
    )

    enum class PriorityLevel { HIGH, MEDIUM, LOW }

    data class SecurityAudit(
        val totalDriversAudited: Int,
        val pendingVerifications: Int,
        val verifiedDeliveryProofs: Int,
        val securityStatus: String,
        val auditAlerts: List<String>
    )

    data class AraFullInsights(
        val analytics: AnalyticsReport,
        val ledger: FinancialLedger,
        val operations: OperationsReport,
        val recommendations: List<RecommendationItem>,
        val security: SecurityAudit
    )

    fun evaluateSystemState(
        orders: List<OrderEntity>,
        drivers: List<DriverEntity>
    ): AraFullInsights {
        // --- 1. ANALYTICS MODULE ---
        val totalOrders = orders.size
        val completed = orders.count { it.status == "Entregado" }
        val pending = orders.count { it.status == "Esperando conductor" || it.status == "Creado" }
        val inTransit = orders.count { it.status == "En camino" || it.status == "Aceptado" }
        val cancelled = orders.count { it.status == "Cancelado" }

        val totalRevenue = orders.filter { it.status != "Cancelado" }.sumOf { it.priceMxn }
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val formattedRevenue = currencyFormat.format(totalRevenue)

        val avgPrice = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
        val completionRate = if (totalOrders > 0) ((completed.toDouble() / totalOrders) * 100).toInt() else 0

        val packageTypeCounts = orders.groupBy { it.packageType }.mapValues { it.value.size }
        val topPackageType = packageTypeCounts.maxByOrNull { it.value }?.key ?: "Sin datos de paquetes"

        val analytics = AnalyticsReport(
            totalOrders = totalOrders,
            completedOrders = completed,
            pendingOrders = pending,
            inTransitOrders = inTransit,
            cancelledOrders = cancelled,
            totalRevenueMxn = totalRevenue,
            formattedRevenue = formattedRevenue,
            averageOrderPriceMxn = avgPrice,
            completionRatePercent = completionRate,
            topPackageType = topPackageType
        )

        // --- 1B. FINANCIAL LEDGER MODULE (ARA CONTADURÍA) ---
        val confirmedOrders = orders.filter { it.isPaymentConfirmed }
        val cashCollected = confirmedOrders.filter { it.paymentMethod == "EFECTIVO" }.sumOf { it.priceMxn }
        val terminalCollected = confirmedOrders.filter { it.paymentMethod == "TERMINAL" }.sumOf { it.priceMxn }
        val speiCollected = confirmedOrders.filter { it.paymentMethod == "TRANSFERENCIA" }.sumOf { it.priceMxn }

        val pendingSpeiOrders = orders.filter { it.paymentMethod == "TRANSFERENCIA" && !it.isPaymentConfirmed }
        val pendingSpeiCount = pendingSpeiOrders.size
        val pendingSpeiAmount = pendingSpeiOrders.sumOf { it.priceMxn }

        val pendingDriverOrders = orders.filter { (it.paymentMethod == "EFECTIVO" || it.paymentMethod == "TERMINAL") && !it.isPaymentConfirmed && it.status != "Cancelado" }
        val pendingDriverCount = pendingDriverOrders.size
        val pendingDriverAmount = pendingDriverOrders.sumOf { it.priceMxn }

        val totalConfirmedMxn = confirmedOrders.sumOf { it.priceMxn }

        val ledger = FinancialLedger(
            cashCollectedMxn = cashCollected,
            terminalCollectedMxn = terminalCollected,
            speiCollectedMxn = speiCollected,
            pendingSpeiCount = pendingSpeiCount,
            pendingSpeiAmountMxn = pendingSpeiAmount,
            pendingDriverCollectionsCount = pendingDriverCount,
            pendingDriverCollectionsMxn = pendingDriverAmount,
            confirmedPaymentsCount = confirmedOrders.size,
            totalRevenueConfirmedMxn = totalConfirmedMxn
        )

        // --- 2. OPERATIONS MODULE ---
        val totalDrivers = drivers.size
        val approvedDrivers = drivers.filter { it.isApproved }
        val pendingDrivers = drivers.filter { !it.isApproved }

        // National coverage: distribution by Mexican state
        val zoneDistribution = mutableMapOf<String, Int>()
        com.example.data.NationalCoverage.STATE_NAMES.forEach { stateName ->
            val count = approvedDrivers.count { it.zone.equals(stateName, ignoreCase = true) || it.coverageZone.equals(stateName, ignoreCase = true) }
            if (count > 0) zoneDistribution[stateName] = count
        }

        val highDemandZones = orders
            .filter { it.status == "Esperando conductor" || it.status == "En camino" }
            .groupBy { order ->
                // Use origin state if available, otherwise infer from address
                if (order.originState.isNotBlank()) order.originState
                else com.example.data.NationalCoverage.STATE_NAMES.find { state ->
                    order.originAddress.contains(state, ignoreCase = true)
                } ?: "Sin estado especificado"
            }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .map { "${it.key} (${it.value} pedidos)" }

        val avgDeliveries = if (approvedDrivers.isNotEmpty()) {
            approvedDrivers.map { it.totalDeliveries }.average().toFloat()
        } else 0.0f

        val operations = OperationsReport(
            totalDrivers = totalDrivers,
            activeApprovedDrivers = approvedDrivers.size,
            pendingApprovalDrivers = pendingDrivers.size,
            zoneDriverDistribution = zoneDistribution,
            highDemandZones = if (highDemandZones.isNotEmpty()) highDemandZones else listOf("Sin demanda acumulada actualmente"),
            averageDeliveriesPerDriver = avgDeliveries
        )

        // --- 3. RECOMMENDATIONS MODULE ---
        val recommendations = mutableListOf<RecommendationItem>()

        if (pendingDrivers.isNotEmpty()) {
            recommendations.add(
                RecommendationItem(
                    title = "Aprobar Socios Conductores Pendientes",
                    description = "Hay ${pendingDrivers.size} solicitud(es) de conductor esperando revisión para aumentar cobertura.",
                    priority = PriorityLevel.HIGH,
                    category = "Fleet Expansion"
                )
            )
        }

        if (pending > 0) {
            recommendations.add(
                RecommendationItem(
                    title = "Optimización de Asignación Cercana",
                    description = "Existen $pending pedido(s) sin asignar. Se sugiere notificar a conductores activos en el estado de origen del envío.",
                    priority = PriorityLevel.HIGH,
                    category = "Operations"
                )
            )
        }

        if (completed > 0) {
            recommendations.add(
                RecommendationItem(
                    title = "Mantenimiento de Calidad de Entrega",
                    description = "El $completionRate% de los pedidos se han completado con éxito. Mantener el protocolo de evidencia de foto y QR.",
                    priority = PriorityLevel.LOW,
                    category = "Quality Assurance"
                )
            )
        } else {
            recommendations.add(
                RecommendationItem(
                    title = "Incentivar Primeras Solicitudes",
                    description = "Promocionar la tarifa inicial de $50 MXN (0-5 km) para captar los primeros envíos locales.",
                    priority = PriorityLevel.MEDIUM,
                    category = "Growth"
                )
            )
        }

        // --- 4. SECURITY MODULE ---
        val verifiedProofs = orders.count { !it.deliveryPhotoUri.isNullOrEmpty() || !it.deliveryQrCode.isNullOrEmpty() }
        val alerts = mutableListOf<String>()

        if (pendingDrivers.isNotEmpty()) {
            alerts.add("${pendingDrivers.size} conductor(es) requieran validación de antecedentes y licencia.")
        }

        val unverifiedCompleted = orders.count { it.status == "Entregado" && it.deliveryPhotoUri.isNullOrEmpty() }
        if (unverifiedCompleted > 0) {
            alerts.add("$unverifiedCompleted pedido(s) entregados no cuentan con foto de evidencia registrada.")
        }

        if (alerts.isEmpty()) {
            alerts.add("Todos los controles de seguridad y verificación están conformes.")
        }

        val security = SecurityAudit(
            totalDriversAudited = totalDrivers,
            pendingVerifications = pendingDrivers.size,
            verifiedDeliveryProofs = verifiedProofs,
            securityStatus = if (alerts.size > 1) "Atención Requerida" else "Óptimo & Protegido",
            auditAlerts = alerts
        )

        return AraFullInsights(
            analytics = analytics,
            ledger = ledger,
            operations = operations,
            recommendations = recommendations,
            security = security
        )
    }
}
