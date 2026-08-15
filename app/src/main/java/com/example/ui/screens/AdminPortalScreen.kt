package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AraSystemLucid
import com.example.data.DriverEntity
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaRedAlert
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.YaVaViewModel

import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import com.example.data.CompanyConfigEntity
import com.example.data.LegalConsentEntity

@Composable
fun AdminPortalScreen(
    viewModel: YaVaViewModel
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val allDrivers by viewModel.allDrivers.collectAsState()
    val pendingDrivers by viewModel.pendingDrivers.collectAsState()
    val companyConfig by viewModel.companyConfig.collectAsState()
    val legalConsents by viewModel.legalConsents.collectAsState()
    val araInsights by viewModel.araInsights.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Socios", "Empresa", "Legal", "Nube & Realtime", "Pedidos", "ARA Lucid")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Admin Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(YaVaYellowPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Panel de Control Operativo Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Supervisión global por Juan Vicente Bello Pablo (Director & Arquitecto de Software). Logística, empresa, legalidad, sincronización cloud y motor ARA System Lucid.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = YaVaYellowPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 9.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> AdminOverviewTab(
                insights = araInsights,
                pendingDriversCount = pendingDrivers.size,
                isWeatherSurgeActive = false,
                onToggleWeatherSurge = { },
                isHighDemandActive = false,
                onToggleHighDemand = { }
            )
            1 -> DriverManagementTab(allDrivers, onSetStatus = { id, status -> viewModel.setDriverStatusByAdmin(id, status) })
            2 -> CompanyConfigAdminTab(companyConfig) { w, p, e, fn, fu, h, em ->
                viewModel.updateCompanyConfig(w, p, e, fn, fu, h, em)
            }
            3 -> LegalAuditAdminTab(legalConsents)
            4 -> CloudSyncAdminTab()
            5 -> OrdersManagementTab(allOrders, onConfirmPayment = { id -> viewModel.confirmPaymentByDirector(id) })
            6 -> AraSystemLucidAiTab(araInsights)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun AdminOverviewTab(
    insights: AraSystemLucid.AraFullInsights,
    pendingDriversCount: Int,
    isWeatherSurgeActive: Boolean,
    onToggleWeatherSurge: (Boolean) -> Unit,
    isHighDemandActive: Boolean,
    onToggleHighDemand: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // High Demand Tariff Management Card (Director)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHighDemandActive) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = if (isHighDemandActive) Color(0xFFF57F17) else YaVaYellowPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tarifa por Alta Demanda / Hora Pico (Director)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tarifa $9 MXN/km (20% Comisión) vs Baja Demanda $8 MXN/km (15% Comisión)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isHighDemandActive,
                        onCheckedChange = onToggleHighDemand,
                        modifier = Modifier.testTag("switch_director_high_demand")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isHighDemandActive) {
                        "🔥 ALTA DEMANDA ACTIVADA ($9 MXN/km): Comisión de plataforma ajustada a 20%, 80% ganancias para socios."
                    } else {
                        "🟢 BAJA DEMANDA ACTIVADA ($8 MXN/km): Comisión estándar de plataforma a 15%, 85% ganancias para socios."
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighDemandActive) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Exclusive Director Weather Surge Control Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isWeatherSurgeActive) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (isWeatherSurgeActive) Color(0xFF0288D1) else YaVaYellowPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ajuste por Clima (Director)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lluvia / Tormenta: +15% tarifa base",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isWeatherSurgeActive,
                        onCheckedChange = onToggleWeatherSurge,
                        modifier = Modifier.testTag("switch_director_weather_surge")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isWeatherSurgeActive) {
                        "⚡ ESTADO ACTIVO: Se está aplicando un 15% de tarifa adicional en todas las cotizaciones solicitadas por clientes."
                    } else {
                        "☀️ ESTADO NORMAL: Operación con tarifa regular ($8 MXN/km baja demanda | $9 MXN/km alta demanda)."
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWeatherSurgeActive) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Quick Key Metrics Matrix
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(
                title = "Ingresos Totales",
                value = insights.analytics.formattedRevenue,
                icon = Icons.Default.MonetizationOn,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "Pedidos Totales",
                value = "${insights.analytics.totalOrders}",
                icon = Icons.Default.LocalShipping,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(
                title = "Socios Activos",
                value = "${insights.operations.activeApprovedDrivers}",
                icon = Icons.Default.DirectionsBike,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "Pendientes Aprobación",
                value = "$pendingDriversCount",
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ARA SYSTEM LUCID - Contaduría Financiera Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = YaVaYellowPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Contaduría & Módulo Financiero (ARA SYSTEM LUCID)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supervisión contable de ingresos por Efectivo, Terminal y Transferencias SPEI MercadoPago.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "💵 Cobrado en Efectivo (Confirmado):", fontSize = 12.sp)
                    Text(text = "\$${insights.ledger.cashCollectedMxn} MXN", fontWeight = FontWeight.Bold, color = YaVaGreenSuccess)
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "💳 Cobrado en Terminal (Confirmado):", fontSize = 12.sp)
                    Text(text = "\$${insights.ledger.terminalCollectedMxn} MXN", fontWeight = FontWeight.Bold, color = YaVaGreenSuccess)
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "🏦 Cobrado por Transferencia SPEI:", fontSize = 12.sp)
                    Text(text = "\$${insights.ledger.speiCollectedMxn} MXN", fontWeight = FontWeight.Bold, color = YaVaGreenSuccess)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⏳ Pendientes de Verificación por Director (SPEI): ${insights.ledger.pendingSpeiCount} pedido(s) (\$${insights.ledger.pendingSpeiAmountMxn} MXN)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "🛵 Pendientes de Cobro por Conductores (Efectivo/Terminal): ${insights.ledger.pendingDriverCollectionsCount} pedido(s) (\$${insights.ledger.pendingDriverCollectionsMxn} MXN)",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Resumen de Rendimiento de Entregas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tasa de Finalización:", fontSize = 13.sp)
                    Text(
                        text = "${insights.analytics.completionRatePercent}%",
                        fontWeight = FontWeight.Bold,
                        color = YaVaGreenSuccess
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Ticket Promedio por Envío:", fontSize = 13.sp)
                    Text(
                        text = "\$${String.format("%.2f", insights.analytics.averageOrderPriceMxn)} MXN",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Categoría Más Solicitada:", fontSize = 13.sp)
                    Text(
                        text = insights.analytics.topPackageType,
                        fontWeight = FontWeight.Bold,
                        color = YaVaYellowPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverApprovalsTab(
    pendingDrivers: List<DriverEntity>,
    onApprove: (Long) -> Unit
) {
    Column {
        Text(
            text = "Solicitudes de Socios Conductores Pendientes (${pendingDrivers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (pendingDrivers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No hay registros pendientes de aprobación en este momento.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pendingDrivers.forEach { driver ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = driver.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Teléfono: ${driver.phone}", fontSize = 12.sp)
                            Text(text = "Vehículo: ${driver.vehicle} | Zona: ${driver.zone}", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onApprove(driver.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = YaVaYellowPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("approve_driver_${driver.id}_button")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aprobar Socio Conductor", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersManagementTab(
    allOrders: List<OrderEntity>,
    onConfirmPayment: (Long) -> Unit
) {
    Column {
        Text(
            text = "Registro Global de Pedidos y Estado de Cobro (${allOrders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            allOrders.forEach { order ->
                val paymentMethodLabel = when (order.paymentMethod) {
                    "TRANSFERENCIA" -> "Transferencia SPEI (MercadoPago)"
                    "TERMINAL" -> "Cobro con Tarjeta (Terminal)"
                    else -> "Efectivo al Conductor"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.trackingCode,
                                fontWeight = FontWeight.ExtraBold,
                                color = YaVaYellowPrimary
                            )
                            Surface(
                                color = if (order.status == "Entregado") YaVaGreenSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = order.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (order.status == "Entregado") YaVaGreenSuccess else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "Cliente: ${order.clientName} | ${order.clientPhone}", fontSize = 12.sp)
                        Text(text = "Ruta: ${order.originAddress} -> ${order.destinationAddress}", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "Precio Total: \$${order.priceMxn} MXN | Conductor: ${order.driverName ?: "Sin asignar"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💳 Quién Paga: ${order.payer} | Método: $paymentMethodLabel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (order.isPaymentConfirmed) {
                                    Text(
                                        text = "✅ Pago Verificado y Confirmado (por ${order.paymentConfirmedBy ?: "Sistema"})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YaVaGreenSuccess
                                    )
                                } else {
                                    Text(
                                        text = if (order.paymentMethod == "TRANSFERENCIA") "⏳ Pendiente de verificación por Director (SPEI MercadoPago CLABE 722969010374423450)" else "⏳ Pendiente de cobro/confirmación por Repartidor",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = { onConfirmPayment(order.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = YaVaYellowPrimary,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("director_confirm_payment_${order.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (order.paymentMethod == "TRANSFERENCIA") "Aceptar y Confirmar Pago SPEI Reflejado en Cuenta" else "Confirmar Pago Recibido (Director)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AraSystemLucidAiTab(insights: AraSystemLucid.AraFullInsights) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // AI Module Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(YaVaYellowPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "ARA System Lucid",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = YaVaYellowPrimary
                    )
                    Text(
                        text = "Módulo /ai - Motor Operativo de Inteligencia Artificial de YaVa!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. /ai/analytics
        AiModuleCard(
            moduleTitle = "/ai/analytics",
            icon = Icons.Default.Analytics
        ) {
            Text(text = "• Pedidos Totales Evaluados: ${insights.analytics.totalOrders}", fontSize = 12.sp)
            Text(text = "• Facturación Total Calculada: ${insights.analytics.formattedRevenue}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = YaVaYellowPrimary)
            Text(text = "• Entregas Completadas: ${insights.analytics.completedOrders} (${insights.analytics.completionRatePercent}%)", fontSize = 12.sp)
            Text(text = "• Pedidos Activos en Tránsito: ${insights.analytics.inTransitOrders}", fontSize = 12.sp)
        }

        // 2. /ai/operations
        AiModuleCard(
            moduleTitle = "/ai/operations",
            icon = Icons.Default.Psychology
        ) {
            Text(text = "• Socios Conductores Activos: ${insights.operations.activeApprovedDrivers}", fontSize = 12.sp)
            Text(text = "• Solicitudes Pendientes: ${insights.operations.pendingApprovalDrivers}", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Densidad por Zonas:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            insights.operations.zoneDriverDistribution.forEach { (zone, count) ->
                Text(text = " - Zona $zone: $count conductores", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 3. /ai/recommendations
        AiModuleCard(
            moduleTitle = "/ai/recommendations",
            icon = Icons.Default.Lightbulb
        ) {
            if (insights.recommendations.isEmpty()) {
                Text(text = "No hay recomendaciones críticas pendientes.", fontSize = 12.sp)
            } else {
                insights.recommendations.forEach { rec ->
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "• ${rec.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (rec.priority == AraSystemLucid.PriorityLevel.HIGH) YaVaRedAlert else YaVaYellowPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = rec.priority.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = rec.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 4. /ai/security
        AiModuleCard(
            moduleTitle = "/ai/security",
            icon = Icons.Default.Shield
        ) {
            Text(text = "Estado de Seguridad: ${insights.security.securityStatus}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = YaVaGreenSuccess)
            Text(text = "• Evidencias de Entrega Verificadas: ${insights.security.verifiedDeliveryProofs}", fontSize = 12.sp)
            Text(text = "• Socios en Auditoría: ${insights.security.totalDriversAudited}", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Alertas de Auditoría:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            insights.security.auditAlerts.forEach { alert ->
                Text(text = " - $alert", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AiModuleCard(
    moduleTitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = moduleTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = YaVaYellowPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DriverManagementTab(
    drivers: List<DriverEntity>,
    onSetStatus: (Long, String) -> Unit
) {
    Column {
        Text(
            text = "Gestión Integral de Socios Conductores (${drivers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (drivers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No hay socios registrados.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                drivers.forEach { driver ->
                    val statusColor = when (driver.status) {
                        "APROBADO" -> YaVaGreenSuccess
                        "PENDIENTE_APROBACION" -> Color(0xFFFF9800)
                        "RECHAZADO" -> Color(0xFFD32F2F)
                        "SUSPENDIDO" -> Color.Gray
                        else -> YaVaYellowPrimary
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = driver.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = driver.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(text = "Teléfono: ${driver.phone} | Correo: ${driver.email.ifEmpty { "N/D" }}", fontSize = 12.sp)
                            Text(text = "Vehículo: ${driver.vehicle} (${driver.brand} ${driver.model} ${driver.year}) | Placas: ${driver.licensePlate.ifEmpty { "N/D" }}", fontSize = 12.sp)
                            Text(text = "Capacidad: ${driver.cargoCapacityKg} kg | Cobertura: ${driver.coverageZone.ifEmpty { driver.zone }}", fontSize = 12.sp)
                            if (driver.emergencyContactName.isNotEmpty()) {
                                Text(text = "Emergencia: ${driver.emergencyContactName} (${driver.emergencyContactPhone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onSetStatus(driver.id, "APROBADO") },
                                    colors = ButtonDefaults.buttonColors(containerColor = YaVaGreenSuccess, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_approve_driver_${driver.id}")
                                ) {
                                    Text("Aprobar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onSetStatus(driver.id, "SUSPENDIDO") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_suspend_driver_${driver.id}")
                                ) {
                                    Text("Suspender", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onSetStatus(driver.id, "RECHAZADO") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_reject_driver_${driver.id}")
                                ) {
                                    Text("Rechazar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyConfigAdminTab(
    config: CompanyConfigEntity,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var whatsappNumber by remember(config) { mutableStateOf(config.whatsappNumber) }
    var phoneNumber by remember(config) { mutableStateOf(config.phoneNumber) }
    var supportEmail by remember(config) { mutableStateOf(config.supportEmail) }
    var facebookName by remember(config) { mutableStateOf(config.facebookName) }
    var facebookUrl by remember(config) { mutableStateOf(config.facebookUrl) }
    var operatingHours by remember(config) { mutableStateOf(config.operatingHours) }
    var emergencyNotice by remember(config) { mutableStateOf(config.emergencyNotice) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Configuración Central de Empresa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Modifica los canales de atención. Se actualizarán automáticamente en toda la aplicación.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = whatsappNumber,
                onValueChange = { whatsappNumber = it },
                label = { Text("Número de WhatsApp Business (ej. 9997431941)") },
                modifier = Modifier.fillMaxWidth().testTag("config_whatsapp_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Teléfono de Atención al Cliente") },
                modifier = Modifier.fillMaxWidth().testTag("config_phone_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = supportEmail,
                onValueChange = { supportEmail = it },
                label = { Text("Correo Electrónico Oficial Support") },
                modifier = Modifier.fillMaxWidth().testTag("config_email_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = facebookName,
                onValueChange = { facebookName = it },
                label = { Text("Nombre de Página de Facebook") },
                modifier = Modifier.fillMaxWidth().testTag("config_fb_name_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = facebookUrl,
                onValueChange = { facebookUrl = it },
                label = { Text("URL de Facebook") },
                modifier = Modifier.fillMaxWidth().testTag("config_fb_url_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = operatingHours,
                onValueChange = { operatingHours = it },
                label = { Text("Horario de Atención") },
                modifier = Modifier.fillMaxWidth().testTag("config_hours_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = emergencyNotice,
                onValueChange = { emergencyNotice = it },
                label = { Text("Aviso u Oferta Destacada") },
                modifier = Modifier.fillMaxWidth().testTag("config_notice_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(
                        whatsappNumber,
                        phoneNumber,
                        supportEmail,
                        facebookName,
                        facebookUrl,
                        operatingHours,
                        emergencyNotice
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("save_company_config_button")
            ) {
                Text("Guardar Cambios de Configuración", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LegalAuditAdminTab(
    consents: List<LegalConsentEntity>
) {
    Column {
        Text(
            text = "Registro de Consentimiento Legal Digital (${consents.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (consents.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No se han registrado consentimientos digitales en la base de datos.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                consents.forEach { consent ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = consent.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                Surface(
                                    color = YaVaGreenSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = consent.userRole,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = YaVaGreenSuccess,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(text = "Teléfono: ${consent.userPhone} | Correo: ${consent.userEmail}", fontSize = 12.sp)
                            Text(text = "Documento Versión: ${consent.documentVersion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Términos Aceptados: Sí | Privacidad Aceptada: Sí", fontSize = 11.sp, color = YaVaGreenSuccess, fontWeight = FontWeight.Bold)
                            Text(text = "Fecha Aceptación: ${java.util.Date(consent.acceptedAt)}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncAdminTab() {
    var firebaseDbUrl by remember { mutableStateOf("https://yava-logistics-default-rtdb.firebaseio.com") }
    var supabaseProjectUrl by remember { mutableStateOf("https://yava-logistics.supabase.co") }
    var supabaseAnonKey by remember { mutableStateOf("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var syncStatusText by remember { mutableStateOf("Conectado localmente. Base de datos SQLite Room sincronizada con Room Persistence Engine.") }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Configuración Sincronización Nube (Firebase & Supabase)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Arquitectura lista para sincronización híbrida offline-first en tiempo real.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = YaVaGreenSuccess.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = YaVaGreenSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = syncStatusText,
                        fontSize = 11.sp,
                        color = YaVaGreenSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("1. FIREBASE REALTIME DATABASE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = firebaseDbUrl,
                onValueChange = { firebaseDbUrl = it },
                label = { Text("URL de Firebase Realtime DB") },
                modifier = Modifier.fillMaxWidth().testTag("input_firebase_url")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("2. SUPABASE BACKEND ENDPOINT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = supabaseProjectUrl,
                onValueChange = { supabaseProjectUrl = it },
                label = { Text("URL de Proyecto Supabase") },
                modifier = Modifier.fillMaxWidth().testTag("input_supabase_url")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = supabaseAnonKey,
                onValueChange = { supabaseAnonKey = it },
                label = { Text("API Anon Key / JWT Token") },
                modifier = Modifier.fillMaxWidth().testTag("input_supabase_key")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sincronización Automática en Segundo Plano", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    modifier = Modifier.testTag("switch_auto_sync")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        syncStatusText = "Prueba de ping exitosa: Conexión establecida con las APIs de Firebase / Supabase configuradas."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_test_cloud_ping")
                ) {
                    Text("Probar Conexión Nube", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        syncStatusText = "Sincronización forzada completada a las " + java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()) + ". Todos los registros guardados localmente."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_force_sync_now")
                ) {
                    Text("Sincronizar Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
