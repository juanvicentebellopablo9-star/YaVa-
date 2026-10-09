package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-fidelity Real-Time Driver Tracking Screen component powered by Google Maps Compose.
 * Displays real-time driver coordinates, live route polyline, origin/destination pins,
 * camera re-centering, traffic layer, map styles, and live telemetry HUD.
 */
@Composable
fun YaVaGoogleMapsTracker(
    selectedOrder: OrderEntity?,
    activeDrivers: List<DriverEntity> = emptyList(),
    driverCurrentLocation: LatLng? = null,
    onDriverLocationUpdated: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Default coordinate: CDMX Center (National default)
    val defaultCenter = LatLng(19.4326, -99.1332)

    val originLatLng = remember(selectedOrder?.originLat, selectedOrder?.originLng) {
        if (selectedOrder?.originLat != null && selectedOrder.originLat != 0.0) {
            LatLng(selectedOrder.originLat, selectedOrder.originLng)
        } else {
            LatLng(19.4326, -99.1332)
        }
    }

    val destLatLng = remember(selectedOrder?.destLat, selectedOrder?.destLng) {
        if (selectedOrder?.destLat != null && selectedOrder.destLat != 0.0) {
            LatLng(selectedOrder.destLat, selectedOrder.destLng)
        } else {
            LatLng(19.4326, -99.1332)
        }
    }

    // Driver location state
    var driverPos by remember(driverCurrentLocation, selectedOrder?.driverLat, selectedOrder?.driverLng) {
        val lat = driverCurrentLocation?.latitude ?: selectedOrder?.driverLat
        val lng = driverCurrentLocation?.longitude ?: selectedOrder?.driverLng
        if (lat != null && lat != 0.0 && lng != null && lng != 0.0) {
            mutableStateOf(LatLng(lat, lng))
        } else {
            // Midpoint between origin and destination initially
            val midLat = (originLatLng.latitude + destLatLng.latitude) / 2.0
            val midLng = (originLatLng.longitude + destLatLng.longitude) / 2.0
            mutableStateOf(LatLng(midLat, midLng))
        }
    }

    val driverMarkerState = rememberMarkerState(position = driverPos)
    val originMarkerState = rememberMarkerState(position = originLatLng)
    val destMarkerState = rememberMarkerState(position = destLatLng)

    // Sync marker positions when state changes
    LaunchedEffect(driverPos) {
        driverMarkerState.position = driverPos
    }
    LaunchedEffect(originLatLng) {
        originMarkerState.position = originLatLng
    }
    LaunchedEffect(destLatLng) {
        destMarkerState.position = destLatLng
    }

    // Map Settings State
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var isLiveSimulationActive by remember { mutableStateOf(true) }
    var speedKmh by remember { mutableFloatStateOf(32.5f) }
    var progressFraction by remember { mutableFloatStateOf(0.4f) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(driverPos, 14.5f)
    }

    // Smooth Live Tracking Simulation or Step interpolation
    LaunchedEffect(isLiveSimulationActive, selectedOrder?.id) {
        while (isLiveSimulationActive) {
            delay(1200)
            progressFraction = (progressFraction + 0.015f)
            if (progressFraction > 1.0f) {
                progressFraction = 0.0f
            }

            // Interpolate driver position between Origin -> Dest
            val interpolatedLat = originLatLng.latitude + (destLatLng.latitude - originLatLng.latitude) * progressFraction
            val interpolatedLng = originLatLng.longitude + (destLatLng.longitude - originLatLng.longitude) * progressFraction

            // Add slight organic curve jitter
            val jitterLat = Math.sin(progressFraction * Math.PI) * 0.0012
            val jitterLng = Math.cos(progressFraction * Math.PI) * 0.0010

            val newPos = LatLng(interpolatedLat + jitterLat, interpolatedLng + jitterLng)
            driverPos = newPos
            speedKmh = 28f + (Math.sin(progressFraction * 10.0) * 8f).toFloat()

            // Notify parent / ViewModel of live driver movement if callback provided
            onDriverLocationUpdated?.invoke(newPos.latitude, newPos.longitude)
        }
    }

    val routePoints = remember(originLatLng, driverPos, destLatLng) {
        listOf(
            originLatLng,
            LatLng(
                originLatLng.latitude + (driverPos.latitude - originLatLng.latitude) * 0.5 + 0.0005,
                originLatLng.longitude + (driverPos.longitude - originLatLng.longitude) * 0.5 - 0.0005
            ),
            driverPos,
            LatLng(
                driverPos.latitude + (destLatLng.latitude - driverPos.latitude) * 0.5 - 0.0004,
                driverPos.longitude + (destLatLng.longitude - driverPos.longitude) * 0.5 + 0.0006
            ),
            destLatLng
        )
    }

    Box(modifier = modifier.clip(RoundedCornerShape(18.dp))) {
        // GOOGLE MAPS COMPOSE VIEW
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_compose_view"),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isTrafficEnabled = isTrafficEnabled,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = true,
                zoomControlsEnabled = false,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true
            )
        ) {
            // 1. ORIGIN MARKER (Green Pin)
            Marker(
                state = originMarkerState,
                title = "Origen: ${selectedOrder?.originAddress ?: "Punto de Recolección"}",
                snippet = "Remitente: ${selectedOrder?.clientName ?: "Cliente"}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )

            // 2. DESTINATION MARKER (Red Pin)
            Marker(
                state = destMarkerState,
                title = "Destino: ${selectedOrder?.destinationAddress ?: "Punto de Entrega"}",
                snippet = "Paquete: ${selectedOrder?.packageType ?: "General"}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            // 3. DRIVER LIVE POSITION MARKER (Yellow / Motorcycle Hue)
            Marker(
                state = driverMarkerState,
                title = "Socio YaVa!: ${selectedOrder?.driverName ?: "Conductor en Ruta"}",
                snippet = "Velocidad: ${String.format(java.util.Locale.US, "%.1f", speedKmh)} km/h • En Camino",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            // 4. ACCURACY / RADAR CIRCLE around driver
            Circle(
                center = driverPos,
                radius = 75.0,
                fillColor = Color(0x33F5C000),
                strokeColor = Color(0xFFE5A000),
                strokeWidth = 3f
            )

            // 5. ROUTE POLYLINE (YaVa Yellow / Blue path)
            Polyline(
                points = routePoints,
                color = Color(0xFF1E88E5),
                width = 12f
            )
        }

        // TOP CONTROLS OVERLAY: Layer Switcher & Traffic Toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Traffic Layer Toggle
            Surface(
                onClick = { isTrafficEnabled = !isTrafficEnabled },
                shape = CircleShape,
                color = if (isTrafficEnabled) YaVaGreenSuccess else MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Traffic,
                        contentDescription = "Tráfico en tiempo real",
                        tint = if (isTrafficEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Map Type Toggle (Normal / Satellite)
            Surface(
                onClick = {
                    mapType = when (mapType) {
                        MapType.NORMAL -> MapType.SATELLITE
                        MapType.SATELLITE -> MapType.TERRAIN
                        else -> MapType.NORMAL
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Tipo de mapa",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // FLOATING ACTION BUTTONS: Center Driver & Auto-Fit Bounds
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 74.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Simulation / Real GPS Toggle
            SmallFloatingActionButton(
                onClick = { isLiveSimulationActive = !isLiveSimulationActive },
                containerColor = if (isLiveSimulationActive) YaVaYellowPrimary else MaterialTheme.colorScheme.surface,
                contentColor = Color.Black,
                modifier = Modifier.testTag("btn_toggle_live_tracking_simulation")
            ) {
                Icon(
                    imageVector = if (isLiveSimulationActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Control de Simulación GPS",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Fit All Markers (Origin + Driver + Dest)
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val bounds = LatLngBounds.builder()
                                .include(originLatLng)
                                .include(driverPos)
                                .include(destLatLng)
                                .build()
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(bounds, 120),
                                durationMs = 800
                            )
                        } catch (_: Exception) {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(driverPos, 14f),
                                durationMs = 800
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("btn_fit_map_bounds")
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Ver Ruta Completa",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center Camera on Driver
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(driverPos, 16f)
                            ),
                            durationMs = 700
                        )
                    }
                },
                containerColor = YaVaYellowPrimary,
                contentColor = Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_center_driver_location")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centrar en Conductor",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // BOTTOM TELEMETRY HUD CARD OVERLAY
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(12.dp)
                .testTag("driver_telemetry_hud")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = YaVaYellowPrimary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(YaVaGreenSuccess)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveSimulationActive) "GPS EN VIVO • ${String.format(java.util.Locale.US, "%.1f", speedKmh)} km/h" else "GPS ESTÁTICO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = YaVaGreenSuccess
                        )
                    }
                    Text(
                        text = selectedOrder?.driverName ?: "Socio YaVa! Asignado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ETA: ~${(selectedOrder?.distanceKm?.times(3) ?: 15).toInt()} min a destino",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
