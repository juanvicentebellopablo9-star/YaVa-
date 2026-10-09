package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import com.example.ui.components.YaVaQrDialog
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OrderEntity
import com.example.ui.components.YaVaContactCard
import com.example.ui.theme.YaVaBlueInfo
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaRedAlert
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.UserRole
import com.example.ui.viewmodel.YaVaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeliveryFilterCategory(val label: String) {
    TODOS("Todos"),
    EN_CAMINO("🚀 En Camino"),
    PENDIENTES("⏳ Esperando"),
    ENTREGADOS("✅ Entregados"),
    TRANSFERENCIA("💳 SPEI")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: YaVaViewModel,
    onNavigateToRequest: () -> Unit,
    onNavigateToTracking: () -> Unit,
    onNavigateToDriverPortal: () -> Unit
) {
    val context = LocalContext.current
    val allOrders by viewModel.allOrders.collectAsState()
    val userName by viewModel.authenticatedUserName.collectAsState()
    val userEmail by viewModel.authenticatedUserEmail.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DeliveryFilterCategory.TODOS) }
    var selectedOrderForQr by remember { mutableStateOf<com.example.data.OrderEntity?>(null) }

    // Metrics calculations
    val activeOrdersCount = remember(allOrders) {
        allOrders.count { it.status != "Entregado" && it.status != "Cancelado" }
    }
    val inTransitCount = remember(allOrders) {
        allOrders.count { it.status.contains("camino", ignoreCase = true) || it.status.contains("transito", ignoreCase = true) }
    }
    val deliveredCount = remember(allOrders) {
        allOrders.count { it.status.equals("Entregado", ignoreCase = true) }
    }
    val totalMxn = remember(allOrders) {
        allOrders.filter { it.status != "Cancelado" }.sumOf { it.priceMxn }
    }

    // Filtered orders list
    val filteredOrders = remember(allOrders, searchQuery, selectedFilter) {
        allOrders.filter { order ->
            val matchesSearch = searchQuery.isBlank() ||
                    order.trackingCode.contains(searchQuery, ignoreCase = true) ||
                    order.originAddress.contains(searchQuery, ignoreCase = true) ||
                    order.destinationAddress.contains(searchQuery, ignoreCase = true) ||
                    order.clientName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedFilter) {
                DeliveryFilterCategory.TODOS -> true
                DeliveryFilterCategory.EN_CAMINO -> order.status.contains("camino", ignoreCase = true) || order.status.contains("transito", ignoreCase = true)
                DeliveryFilterCategory.PENDIENTES -> order.status.contains("esperando", ignoreCase = true) || order.status.contains("creado", ignoreCase = true)
                DeliveryFilterCategory.ENTREGADOS -> order.status.equals("Entregado", ignoreCase = true)
                DeliveryFilterCategory.TRANSFERENCIA -> order.paymentMethod.equals("TRANSFERENCIA", ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Minimalist Hero Section Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.yava_hero_banner_1785394303763),
                            contentDescription = "YaVa Hero Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        ) {
                            Surface(
                                color = YaVaYellowPrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "TARIFA DINÁMICA TRANSPARENTE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_logo_1785438590813),
                                    contentDescription = "Logo YaVa!",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, YaVaYellowPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Panel de Envíos YaVa!",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Hola, $userName • Cobertura Nacional 🇲🇽",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dashboard Metric Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCounterCard(
                    title = "Activos",
                    value = activeOrdersCount.toString(),
                    icon = Icons.Default.LocalShipping,
                    badgeColor = YaVaYellowPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterCard(
                    title = "En Camino",
                    value = inTransitCount.toString(),
                    icon = Icons.Default.TwoWheeler,
                    badgeColor = YaVaBlueInfo,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterCard(
                    title = "Entregados",
                    value = deliveredCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    badgeColor = YaVaGreenSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterCard(
                    title = "Total",
                    value = "\$${"%.0f".format(totalMxn)}",
                    icon = Icons.Default.ReceiptLong,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions & New Order CTA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YaVaYellowPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("action_solicitar_envio_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nuevo Envío", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToTracking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("action_ver_mapa_general_button")
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mapa en Vivo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section Title & Search Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(YaVaYellowPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entregas & Rastreo en Tiempo Real",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${filteredOrders.size} envíos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por código (YAVA-...), dirección o cliente...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = YaVaYellowPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YaVaYellowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_deliveries_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeliveryFilterCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedFilter == category,
                            onClick = { selectedFilter = category },
                            label = { Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YaVaYellowPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Deliveries List
        if (filteredOrders.isEmpty()) {
            item {
                EmptyDeliveriesState(
                    onNewOrder = onNavigateToRequest,
                    hasFilter = searchQuery.isNotBlank() || selectedFilter != DeliveryFilterCategory.TODOS
                )
            }
        } else {
            items(filteredOrders, key = { it.trackingCode }) { order ->
                ActiveDeliveryCard(
                    order = order,
                    onTrackOnMap = {
                        viewModel.selectOrderForTracking(order.trackingCode)
                        onNavigateToTracking()
                    },
                    onShowQr = {
                        selectedOrderForQr = order
                    },
                    onCopyTracking = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("YaVa Tracking Code", order.trackingCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Código ${order.trackingCode} copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    onContactDriver = { phone ->
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo iniciar la llamada", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Live Leaflet OSM Quick Preview Banner
        item {
            LiveMapShortcutCard(
                activeOrdersCount = activeOrdersCount,
                onOpenMap = onNavigateToTracking
            )
        }

        // Direct Contact Card
        item {
            YaVaContactCard(
                modifier = Modifier.fillMaxWidth(),
                customMessage = "Hola YaVa!, tengo una duda sobre el estado de mis envíos."
            )
        }

        // Subtle Attribution Footer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "YaVa! Logistics Nacional 🇲🇽 • Desarrollado por Ing. Vicente Bello",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("subtle_attribution_footer")
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (selectedOrderForQr != null) {
        YaVaQrDialog(
            order = selectedOrderForQr!!,
            onDismiss = { selectedOrderForQr = null }
        )
    }
}

@Composable
private fun MetricCounterCard(
    title: String,
    value: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActiveDeliveryCard(
    order: OrderEntity,
    onTrackOnMap: () -> Unit,
    onShowQr: () -> Unit,
    onCopyTracking: () -> Unit,
    onContactDriver: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val isEnCamino = order.status.contains("camino", ignoreCase = true) || order.status.contains("transito", ignoreCase = true)
    val isEntregado = order.status.equals("Entregado", ignoreCase = true)
    val isEsperando = order.status.contains("esperando", ignoreCase = true) || order.status.contains("creado", ignoreCase = true)

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delivery_item_card_${order.trackingCode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Tracking Code & Dynamic Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tracking Code Badge
                Surface(
                    color = YaVaYellowPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onCopyTracking() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = order.trackingCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = YaVaYellowPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Código",
                            tint = YaVaYellowPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Status Badge with icon & animation
                DeliveryStatusBadge(
                    status = order.status,
                    isEnCamino = isEnCamino,
                    isEntregado = isEntregado,
                    isEsperando = isEsperando,
                    pulseAlpha = pulseAlpha
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Timeline Visual (Origin -> Destination)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Route Visual Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp, end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(YaVaGreenSuccess)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(26.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(YaVaRedAlert)
                    )
                }

                // Addresses Column
                Column(modifier = Modifier.weight(1f)) {
                    // Origin Address
                    Column {
                        Text(
                            text = "RECOLECCIÓN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = YaVaGreenSuccess
                        )
                        Text(
                            text = order.originAddress,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Destination Address
                    Column {
                        Text(
                            text = "ENTREGA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = YaVaRedAlert
                        )
                        Text(
                            text = order.destinationAddress,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Financial & Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = YaVaYellowPrimary
                    ) {
                        Text(
                            text = "\$${"%.2f".format(order.priceMxn)} MXN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${order.distanceKm} km • ~${order.estimatedTimeMinutes} min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Payment Method Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (order.paymentMethod) {
                        "TRANSFERENCIA" -> YaVaBlueInfo.copy(alpha = 0.15f)
                        "TERMINAL" -> YaVaGreenSuccess.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = when (order.paymentMethod) {
                                "TRANSFERENCIA" -> YaVaBlueInfo
                                "TERMINAL" -> YaVaGreenSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = when (order.paymentMethod) {
                                "TRANSFERENCIA" -> "SPEI MercadoPago"
                                "TERMINAL" -> "Tarjeta"
                                else -> "Efectivo"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (order.paymentMethod) {
                                "TRANSFERENCIA" -> YaVaBlueInfo
                                "TERMINAL" -> YaVaGreenSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Driver Information Badge if assigned
            if (!order.driverName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(YaVaYellowPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = order.driverName ?: "Socio Repartidor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Socio YaVa! Asignado",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!order.driverPhone.isNullOrBlank()) {
                            IconButton(
                                onClick = { onContactDriver(order.driverPhone ?: "") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Llamar",
                                    tint = YaVaGreenSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Additional Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            DetailRow(label = "Cliente:", value = "${order.clientName} (${order.clientPhone})")
                            DetailRow(label = "Tipo Paquete:", value = "${order.packageType} • ${order.weightKg} kg")
                            DetailRow(label = "Pagador:", value = order.payer)
                            if (order.notes.isNotBlank()) {
                                DetailRow(label = "Instrucciones:", value = order.notes)
                            }
                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
                            DetailRow(label = "Fecha de Solicitud:", value = dateStr)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Action Buttons: Direct Map Tracking Link & Details Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prominent Map Tracking Button
                Button(
                    onClick = onTrackOnMap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YaVaYellowPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_track_on_map_${order.trackingCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Rastrear en Mapa",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rastrear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Dedicated QR Code Button
                OutlinedButton(
                    onClick = onShowQr,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("btn_show_qr_${order.trackingCode}")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Ver QR",
                        tint = YaVaYellowPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Details Toggle Button
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Menos" else "Detalles",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryStatusBadge(
    status: String,
    isEnCamino: Boolean,
    isEntregado: Boolean,
    isEsperando: Boolean,
    pulseAlpha: Float
) {
    val (bgColor, textColor, icon) = when {
        isEntregado -> Triple(YaVaGreenSuccess, Color.White, Icons.Default.CheckCircle)
        isEnCamino -> Triple(YaVaYellowPrimary, Color.Black, Icons.Default.DirectionsBike)
        isEsperando -> Triple(Color(0xFFF97316), Color.White, Icons.Default.HourglassEmpty)
        status.contains("aceptado", ignoreCase = true) -> Triple(YaVaBlueInfo, Color.White, Icons.Default.Shield)
        else -> Triple(Color.Gray, Color.White, Icons.Default.Inventory2)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isEnCamino) bgColor.copy(alpha = pulseAlpha) else bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    isEnCamino -> "En Camino"
                    isEntregado -> "Entregado"
                    isEsperando -> "Esperando Socio"
                    else -> status
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun EmptyDeliveriesState(
    onNewOrder: () -> Unit,
    hasFilter: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(YaVaYellowPrimary.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasFilter) "No se encontraron envíos con este filtro" else "No tienes entregas activas en este momento",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Solicita envíos a nivel nacional con cotización en tiempo real y 15% de comisión transparente.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onNewOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YaVaYellowPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Solicitar Envío Ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveMapShortcutCard(
    activeOrdersCount: Int,
    onOpenMap: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMap() }
            .testTag("live_map_shortcut_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(YaVaYellowPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Mapa",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Rastreo Satelital & Leaflet OSM",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (activeOrdersCount > 0) "$activeOrdersCount entrega(s) en seguimiento nacional" else "Mapa interactivo nacional y Flota YaVa!",
                        fontSize = 11.sp,
                        color = YaVaYellowPrimary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Abrir Mapa",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
