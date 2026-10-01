package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.CloudOrder
import com.example.ai.FirestoreDeliverySnapshot
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary

/**
 * Real-time Status Tracker for Senders powered by Cloud Firestore Snapshots.
 * Displays live delivery milestone progression, driver telemetry, Firestore snapshot metadata,
 * and sender quick actions (QR display, sharing, direct contact, receipt).
 */
@Composable
fun RealtimeDeliveryProgressTracker(
    order: OrderEntity,
    realtimeSnapshot: FirestoreDeliverySnapshot?,
    onShowQr: () -> Unit,
    onShowReceipt: () -> Unit,
    onSimulateSnapshotAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine current effective state (prefer live Firestore snapshot if available)
    val liveStatus = realtimeSnapshot?.order?.status ?: order.status
    val liveDriverName = realtimeSnapshot?.order?.driverName ?: order.driverName
    val liveDriverLat = realtimeSnapshot?.order?.driverLat ?: order.driverLat
    val liveDriverLng = realtimeSnapshot?.order?.driverLng ?: order.driverLng
    val liveProgressPercent = when (liveStatus) {
        "Creado", "Esperando conductor" -> 20
        "Aceptado" -> 50
        "En camino" -> 80
        "Entregado" -> 100
        else -> 20
    }

    val animatedProgress by animateFloatAsState(
        targetValue = liveProgressPercent / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    // Pulsing animation for the Firestore Live Snapshot badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val snapshotSource = realtimeSnapshot?.metadata?.source ?: "FIRESTORE_SNAPSHOT_READY"
    val isFromCache = realtimeSnapshot?.metadata?.isFromCache ?: false

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_delivery_tracker_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header: Live Firestore Status Indicator & Snapshot Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Pulsing Live Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(YaVaGreenSuccess.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EN VIVO",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = YaVaGreenSuccess
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• FIRESTORE SNAPSHOT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "yava_orders/${order.trackingCode}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Right: Source Badge (Server / Cache)
                Surface(
                    color = if (isFromCache) Color(0xFFFFF3E0) else YaVaGreenSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFromCache) Icons.Default.CloudDone else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isFromCache) Color(0xFFE65100) else YaVaGreenSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFromCache) "Caché Activo" else "Stream Cloud",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFromCache) Color(0xFFE65100) else YaVaGreenSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Delivery Progress Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Progreso del Envío",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$liveProgressPercent%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = YaVaYellowPrimary
                            )
                        }

                        // Prominent Status Pill
                        Surface(
                            color = when (liveStatus) {
                                "Entregado" -> YaVaGreenSuccess
                                "En camino" -> YaVaYellowPrimary
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = liveStatus.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = when (liveStatus) {
                                    "Entregado" -> Color.White
                                    "En camino" -> Color.Black
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Progress Bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (liveStatus == "Entregado") YaVaGreenSuccess else YaVaYellowPrimary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Human-Readable Stage Explanation
                    val stageDescription = when (liveStatus) {
                        "Creado", "Esperando conductor" -> "Tu solicitud está registrada y esperando asignación de socio conductor."
                        "Aceptado" -> "${liveDriverName ?: "El socio conductor"} aceptó tu pedido y se dirige al origen de recolección."
                        "En camino" -> "Tu paquete ya fue recolectado y se encuentra en ruta activa hacia el destino."
                        "Entregado" -> "¡Entrega completada exitosamente! Evidencia y firma digital registradas."
                        else -> "Servicio en proceso de actualización en la nube."
                    }

                    Text(
                        text = stageDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Stage Visual Milestones Stepper
            DeliveryMilestonesTimeline(currentStatus = liveStatus)

            Spacer(modifier = Modifier.height(16.dp))

            // Telemetry & Driver Details (Visible if driver assigned)
            if (liveDriverName != null || liveStatus == "Aceptado" || liveStatus == "En camino" || liveStatus == "Entregado") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(YaVaYellowPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBike,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = liveDriverName ?: "Socio Conductor YaVa!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Calificación: 4.9 ⭐ | Motocicleta Eléctrica",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Direct Contact Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:9991234567"))
                                        context.startActivity(dialIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Llamar",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // GPS Telemetry row streamed from Firestore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = YaVaYellowPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Telemetría GPS: ${String.format("%.4f", liveDriverLat ?: 20.9674)}, ${String.format("%.4f", liveDriverLng ?: -89.6237)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "ETA: ~${order.estimatedTimeMinutes} min",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = YaVaGreenSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Sender Quick Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: QR & Receipt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShowQr,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YaVaYellowPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sender_show_qr_button")
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Código QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShowReceipt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sender_show_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recibo Digital", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Row 2: Share Live Tracking & Advance Simulator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "📦 Sigue mi envío en tiempo real con YaVa! Logistics.\n" +
                                    "Código de Guía: ${order.trackingCode}\n" +
                                    "Estado Actual: $liveStatus ($liveProgressPercent%)\n" +
                                    "Ruta: ${order.originAddress} -> ${order.destinationAddress}\n" +
                                    "Monitoreo en vivo vía Firestore Snapshots."
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Rastreo en Tiempo Real"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sender_share_tracking_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    // Interactive Simulator Button: Advances Firestore snapshot step in real time!
                    Button(
                        onClick = {
                            onSimulateSnapshotAdvance()
                            Toast.makeText(context, "⚡ Firestore Snapshot emitido: Estado actualizado", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_firestore_snapshot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = YaVaYellowPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simular Snapshot", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Visual 4-Stage Milestones Component showing exact sender progression.
 */
@Composable
private fun DeliveryMilestonesTimeline(currentStatus: String) {
    data class Milestone(val label: String, val icon: ImageVector, val statusMatch: List<String>)

    val milestones = listOf(
        Milestone("Confirmado", Icons.Default.CheckCircle, listOf("Creado", "Esperando conductor", "Aceptado", "En camino", "Entregado")),
        Milestone("Socio Asignado", Icons.Default.DirectionsBike, listOf("Aceptado", "En camino", "Entregado")),
        Milestone("En Tránsito", Icons.Default.LocalShipping, listOf("En camino", "Entregado")),
        Milestone("Entregado", Icons.Default.CloudDone, listOf("Entregado"))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        milestones.forEachIndexed { index, milestone ->
            val isPassed = milestone.statusMatch.contains(currentStatus)
            val isCurrent = when (index) {
                0 -> currentStatus in listOf("Creado", "Esperando conductor")
                1 -> currentStatus == "Aceptado"
                2 -> currentStatus == "En camino"
                3 -> currentStatus == "Entregado"
                else -> false
            }

            val stepColor by animateColorAsState(
                targetValue = when {
                    isCurrent -> YaVaYellowPrimary
                    isPassed -> YaVaGreenSuccess
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "milestoneColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(stepColor)
                ) {
                    Icon(
                        imageVector = milestone.icon,
                        contentDescription = milestone.label,
                        tint = if (isPassed || isCurrent) Color.Black else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = milestone.label,
                    fontSize = 10.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) YaVaYellowPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            if (index < milestones.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.5f)
                        .background(
                            if (milestones[index + 1].statusMatch.contains(currentStatus)) YaVaGreenSuccess
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}
