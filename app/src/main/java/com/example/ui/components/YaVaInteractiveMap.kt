package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaRedAlert
import com.example.ui.theme.YaVaYellowPrimary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

// Geographic Boundary of Mérida Metropolitan Area, Yucatán, Mexico
val MERIDA_CENTER_LATLNG = LatLng(20.9674, -89.6237)
private const val MERIDA_MIN_LAT = 20.8800
private const val MERIDA_MAX_LAT = 21.0800
private const val MERIDA_MIN_LNG = -89.7200
private const val MERIDA_MAX_LNG = -89.5200

private fun latLngToOffset(
    lat: Double,
    lng: Double,
    width: Float,
    height: Float,
    zoomScale: Float
): Offset {
    val normX = ((lng - MERIDA_MIN_LNG) / (MERIDA_MAX_LNG - MERIDA_MIN_LNG)).toFloat().coerceIn(0f, 1f)
    val normY = (1.0 - ((lat - MERIDA_MIN_LAT) / (MERIDA_MAX_LAT - MERIDA_MIN_LAT))).toFloat().coerceIn(0f, 1f)

    val centerX = width / 2f
    val centerY = height / 2f

    val rawX = normX * width
    val rawY = normY * height

    val x = centerX + (rawX - centerX) * zoomScale
    val y = centerY + (rawY - centerY) * zoomScale

    return Offset(x, y)
}

private fun offsetToLatLng(
    offset: Offset,
    width: Float,
    height: Float,
    zoomScale: Float
): Pair<Double, Double> {
    val centerX = width / 2f
    val centerY = height / 2f

    val rawX = centerX + (offset.x - centerX) / zoomScale
    val rawY = centerY + (offset.y - centerY) / zoomScale

    val normX = (rawX / width).coerceIn(0f, 1f)
    val normY = (rawY / height).coerceIn(0f, 1f)

    val lng = MERIDA_MIN_LNG + normX * (MERIDA_MAX_LNG - MERIDA_MIN_LNG)
    val lat = MERIDA_MAX_LAT - normY * (MERIDA_MAX_LAT - MERIDA_MIN_LAT)

    return Pair(lat, lng)
}

enum class MapEngine {
    GOOGLE_MAPS,
    MERIDA_VECTOR
}

@Composable
fun YaVaInteractiveMap(
    modifier: Modifier = Modifier,
    selectedOrder: OrderEntity? = null,
    activeDrivers: List<DriverEntity> = emptyList(),
    driverCurrentLocation: LatLng? = null,
    onPointSelected: ((Double, Double) -> Unit)? = null
) {
    var currentEngine by remember { mutableStateOf(MapEngine.GOOGLE_MAPS) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Google Maps Camera state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MERIDA_CENTER_LATLNG, 13f)
    }

    LaunchedEffect(selectedOrder, driverCurrentLocation) {
        val targetPos = when {
            driverCurrentLocation != null -> driverCurrentLocation
            selectedOrder != null && selectedOrder.destLat != 0.0 -> LatLng(selectedOrder.destLat, selectedOrder.destLng)
            selectedOrder != null && selectedOrder.originLat != 0.0 -> LatLng(selectedOrder.originLat, selectedOrder.originLng)
            else -> MERIDA_CENTER_LATLNG
        }
        try {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(targetPos, 14f),
                durationMs = 800
            )
        } catch (_: Exception) {
            // Ignore camera animate exception in background
        }
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = true
            )
        )
    }

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false
            )
        )
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.testTag("interactive_map_canvas")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (currentEngine == MapEngine.GOOGLE_MAPS) {
                // Google Maps Rendering Engine
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = mapUiSettings,
                    properties = mapProperties,
                    onMapClick = { latLng ->
                        onPointSelected?.invoke(latLng.latitude, latLng.longitude)
                    }
                ) {
                    if (selectedOrder != null) {
                        val originLat = if (selectedOrder.originLat != 0.0) selectedOrder.originLat else 20.9850
                        val originLng = if (selectedOrder.originLng != 0.0) selectedOrder.originLng else -89.6180
                        val originLatLng = LatLng(originLat, originLng)

                        Marker(
                            state = remember(originLatLng) { MarkerState(position = originLatLng) },
                            title = "Punto de Recolección (Origen)",
                            snippet = selectedOrder.originAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )

                        val destLat = if (selectedOrder.destLat != 0.0) selectedOrder.destLat else 21.0188
                        val destLng = if (selectedOrder.destLng != 0.0) selectedOrder.destLng else -89.5840
                        val destLatLng = LatLng(destLat, destLng)

                        Marker(
                            state = remember(destLatLng) { MarkerState(position = destLatLng) },
                            title = "Punto de Entrega (Destino)",
                            snippet = selectedOrder.destinationAddress,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )

                        val actualDriverPos = driverCurrentLocation ?: LatLng(
                            (originLat + destLat) / 2.0,
                            (originLng + destLng) / 2.0
                        )

                        Marker(
                            state = remember(actualDriverPos) { MarkerState(position = actualDriverPos) },
                            title = "Socio Repartidor YaVa! (${selectedOrder.driverName ?: "En Camino"})",
                            snippet = "Tel: ${selectedOrder.driverPhone ?: "N/A"} | Guía: ${selectedOrder.trackingCode}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        )

                        Polyline(
                            points = listOf(originLatLng, actualDriverPos, destLatLng),
                            color = Color(0xFFF59E0B),
                            width = 12f,
                            geodesic = true
                        )
                    }

                    activeDrivers.forEachIndexed { index, driver ->
                        val driverPos = LatLng(
                            20.9600 + ((index * 0.018) % 0.09),
                            -89.6300 + ((index * 0.022) % 0.10)
                        )

                        Marker(
                            state = remember(driverPos) { MarkerState(position = driverPos) },
                            title = "Repartidor: ${driver.fullName}",
                            snippet = "${driver.vehicle} | ${driver.licensePlate} | ${if (driver.isAvailable) "Disponible" else "En Servicio"}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (driver.isAvailable) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ORANGE
                            )
                        )
                    }
                }
            } else {
                // Vector Map of Mérida Metropolitan Area Engine
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val (lat, lng) = offsetToLatLng(offset, size.width.toFloat(), size.height.toFloat(), zoomScale)
                                onPointSelected?.invoke(lat, lng)
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    drawRect(color = Color(0xFF18181C))

                    val gridSpacing = 35.dp.toPx() * zoomScale
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = Color(0xFF26262E),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }

                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = Color(0xFF26262E),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }

                    // Anillo Periférico de Mérida
                    val perifericoCenter = latLngToOffset(20.9700, -89.6200, width, height, zoomScale)
                    val perifericoRadiusX = (width * 0.35f) * zoomScale
                    val perifericoRadiusY = (height * 0.35f) * zoomScale

                    drawOval(
                        color = Color(0xFF3F3F46),
                        topLeft = Offset(perifericoCenter.x - perifericoRadiusX, perifericoCenter.y - perifericoRadiusY),
                        size = Size(perifericoRadiusX * 2, perifericoRadiusY * 2),
                        style = Stroke(width = 8.dp.toPx() * zoomScale)
                    )

                    // Paseo de Montejo y Prolongación Montejo
                    val centroPos = latLngToOffset(20.9674, -89.6237, width, height, zoomScale)
                    val montejoNorthPos = latLngToOffset(21.0360, -89.6350, width, height, zoomScale)

                    drawLine(
                        color = Color(0xFF52525B),
                        start = centroPos,
                        end = montejoNorthPos,
                        strokeWidth = 6.dp.toPx() * zoomScale
                    )

                    // Carretera Mérida - Progreso
                    val progresoPos = latLngToOffset(21.0800, -89.6450, width, height, zoomScale)
                    drawLine(
                        color = Color(0xFF52525B),
                        start = montejoNorthPos,
                        end = progresoPos,
                        strokeWidth = 7.dp.toPx() * zoomScale
                    )

                    // Corredor Altabrisa & Av. García Lavín
                    val altabrisaPos = latLngToOffset(21.0188, -89.5840, width, height, zoomScale)
                    val garciaLavinPos = latLngToOffset(21.0280, -89.5980, width, height, zoomScale)

                    drawLine(
                        color = Color(0xFF3F3F46),
                        start = garciaLavinPos,
                        end = altabrisaPos,
                        strokeWidth = 5.dp.toPx() * zoomScale
                    )

                    // Ciudad Caucel (Poniente)
                    val caucelPos = latLngToOffset(20.9980, -89.7020, width, height, zoomScale)
                    drawLine(
                        color = Color(0xFF3F3F46),
                        start = centroPos,
                        end = caucelPos,
                        strokeWidth = 5.dp.toPx() * zoomScale
                    )

                    // Kanasín (Oriente)
                    val kanasinPos = latLngToOffset(20.9333, -89.5583, width, height, zoomScale)
                    drawLine(
                        color = Color(0xFF3F3F46),
                        start = centroPos,
                        end = kanasinPos,
                        strokeWidth = 5.dp.toPx() * zoomScale
                    )

                    // Active Order Route & Driver Pin
                    if (selectedOrder != null) {
                        val originLat = if (selectedOrder.originLat != 0.0) selectedOrder.originLat else 20.9850
                        val originLng = if (selectedOrder.originLng != 0.0) selectedOrder.originLng else -89.6180

                        val destLat = if (selectedOrder.destLat != 0.0) selectedOrder.destLat else 21.0188
                        val destLng = if (selectedOrder.destLng != 0.0) selectedOrder.destLng else -89.5840

                        val originOffset = latLngToOffset(originLat, originLng, width, height, zoomScale)
                        val destOffset = latLngToOffset(destLat, destLng, width, height, zoomScale)

                        val routePath = Path().apply {
                            moveTo(originOffset.x, originOffset.y)
                            val midX = (originOffset.x + destOffset.x) / 2f + 30f
                            val midY = (originOffset.y + destOffset.y) / 2f - 40f
                            quadraticTo(midX, midY, destOffset.x, destOffset.y)
                        }

                        drawPath(
                            path = routePath,
                            color = YaVaYellowPrimary,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f)
                            )
                        )

                        val actualDriverLat = driverCurrentLocation?.latitude ?: ((originLat + destLat) / 2.0)
                        val actualDriverLng = driverCurrentLocation?.longitude ?: ((originLng + destLng) / 2.0)
                        val driverCurrentPos = latLngToOffset(actualDriverLat, actualDriverLng, width, height, zoomScale)

                        drawCircle(
                            color = YaVaYellowPrimary.copy(alpha = 0.35f * (1f - pulseProgress)),
                            radius = 36.dp.toPx() * pulseProgress,
                            center = driverCurrentPos
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 14.dp.toPx(),
                            center = driverCurrentPos
                        )
                        drawCircle(
                            color = YaVaYellowPrimary,
                            radius = 10.dp.toPx(),
                            center = driverCurrentPos
                        )

                        // Origin Pin
                        drawCircle(color = YaVaGreenSuccess, radius = 14.dp.toPx(), center = originOffset)
                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = originOffset)

                        // Destination Pin
                        drawCircle(color = YaVaRedAlert, radius = 14.dp.toPx(), center = destOffset)
                        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = destOffset)
                    }

                    // Available Driver Fleet
                    activeDrivers.forEachIndexed { index, _ ->
                        val dLat = 20.9500 + ((index * 0.025) % 0.12)
                        val dLng = -89.6500 + ((index * 0.035) % 0.15)
                        val driverPos = latLngToOffset(dLat, dLng, width, height, zoomScale)

                        drawCircle(color = YaVaYellowPrimary.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = driverPos)
                        drawCircle(color = Color.Black, radius = 10.dp.toPx(), center = driverPos)
                        drawCircle(color = YaVaYellowPrimary, radius = 6.dp.toPx(), center = driverPos)
                    }
                }
            }

            // Top Status Bar Badge Overlay with Engine Switcher
            Surface(
                color = Color.Black.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(YaVaGreenSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mérida, Yucatán - GPS Satelital",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (selectedOrder != null) "Rastreo activo: ${selectedOrder.trackingCode}" else "Cobertura Metropolitana Activa",
                                fontSize = 9.sp,
                                color = YaVaYellowPrimary
                            )
                        }
                    }

                    // Engine Toggle Button
                    Surface(
                        onClick = {
                            currentEngine = if (currentEngine == MapEngine.GOOGLE_MAPS) {
                                MapEngine.MERIDA_VECTOR
                            } else {
                                MapEngine.GOOGLE_MAPS
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Cambiar vista",
                                tint = YaVaYellowPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentEngine == MapEngine.GOOGLE_MAPS) "Google Maps" else "Mapa Vectorial",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Controls on bottom-right (Zoom in/out / Reset)
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
            ) {
                FloatingMapControl(
                    icon = Icons.Default.Add,
                    contentDesc = "Zoom in",
                    onClick = {
                        if (currentEngine == MapEngine.GOOGLE_MAPS) {
                            cameraPositionState.move(CameraUpdateFactory.zoomIn())
                        } else {
                            zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                FloatingMapControl(
                    icon = Icons.Default.Remove,
                    contentDesc = "Zoom out",
                    onClick = {
                        if (currentEngine == MapEngine.GOOGLE_MAPS) {
                            cameraPositionState.move(CameraUpdateFactory.zoomOut())
                        } else {
                            zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.6f)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                FloatingMapControl(
                    icon = Icons.Default.MyLocation,
                    contentDesc = "Centrar en Mérida",
                    onClick = {
                        if (currentEngine == MapEngine.GOOGLE_MAPS) {
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(MERIDA_CENTER_LATLNG, 13f))
                        } else {
                            zoomScale = 1.0f
                        }
                    }
                )
            }

            // Bottom Legend Overlay
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapLegendItem(color = YaVaGreenSuccess, label = "Origen")
                    Spacer(modifier = Modifier.width(8.dp))
                    MapLegendItem(color = YaVaRedAlert, label = "Destino")
                    Spacer(modifier = Modifier.width(8.dp))
                    MapLegendItem(color = YaVaYellowPrimary, label = "Repartidor")
                }
            }
        }
    }
}

@Composable
private fun FloatingMapControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MapLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.LightGray
        )
    }
}
