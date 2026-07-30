package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun YaVaInteractiveMap(
    modifier: Modifier = Modifier,
    selectedOrder: OrderEntity? = null,
    activeDrivers: List<DriverEntity> = emptyList(),
    onPointSelected: ((Double, Double) -> Unit)? = null
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.testTag("interactive_map_canvas")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val normLat = 19.4 + (1 - offset.y / size.height) * 0.1
                            val normLng = -99.2 + (offset.x / size.width) * 0.1
                            onPointSelected?.invoke(normLat, normLng)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw Dark Grid City Map Background
                drawRect(color = Color(0xFF1E1E22))

                // Draw City Grid Lines
                val gridSpacing = 40.dp.toPx() * zoomScale
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = Color(0xFF2C2C32),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color(0xFF2C2C32),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }

                // Draw City Zones (Centro, Norte, Sur, etc.)
                drawRect(
                    color = Color(0xFF2B2818).copy(alpha = 0.4f),
                    topLeft = Offset(width * 0.25f, height * 0.25f),
                    size = Size(width * 0.5f, height * 0.5f)
                )

                // Main Avenues/Highways
                val avenuePath = Path().apply {
                    moveTo(width * 0.1f, height * 0.8f)
                    cubicTo(
                        width * 0.3f, height * 0.2f,
                        width * 0.7f, height * 0.9f,
                        width * 0.9f, height * 0.3f
                    )
                }
                drawPath(
                    path = avenuePath,
                    color = Color(0xFF3F3F46),
                    style = Stroke(width = 12.dp.toPx() * zoomScale)
                )

                // Draw Order Route Path if order exists
                if (selectedOrder != null) {
                    val originOffset = Offset(width * 0.3f, height * 0.65f)
                    val destinationOffset = Offset(width * 0.75f, height * 0.35f)

                    // Curve route path
                    val routePath = Path().apply {
                        moveTo(originOffset.x, originOffset.y)
                        quadraticTo(
                            (originOffset.x + destinationOffset.x) / 2,
                            originOffset.y - 100f,
                            destinationOffset.x,
                            destinationOffset.y
                        )
                    }

                    // Route line glowing yellow
                    drawPath(
                        path = routePath,
                        color = YaVaYellowPrimary.copy(alpha = 0.8f),
                        style = Stroke(
                            width = 6.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                        )
                    )

                    // Moving animated delivery vehicle along path
                    val currentPos = Offset(
                        x = originOffset.x + (destinationOffset.x - originOffset.x) * (pulseProgress),
                        y = originOffset.y + (destinationOffset.y - originOffset.y) * (pulseProgress)
                    )

                    // Pulse ring around courier
                    drawCircle(
                        color = YaVaYellowPrimary.copy(alpha = 0.3f * (1f - pulseProgress)),
                        radius = 30.dp.toPx() * pulseProgress,
                        center = currentPos
                    )

                    drawCircle(
                        color = YaVaYellowPrimary,
                        radius = 8.dp.toPx(),
                        center = currentPos
                    )

                    // Origin Pin (Green)
                    drawCircle(
                        color = YaVaGreenSuccess,
                        radius = 12.dp.toPx(),
                        center = originOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = originOffset
                    )

                    // Destination Pin (Red)
                    drawCircle(
                        color = YaVaRedAlert,
                        radius = 12.dp.toPx(),
                        center = destinationOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = destinationOffset
                    )
                }

                // Draw Active Drivers on Map
                activeDrivers.forEachIndexed { index, driver ->
                    val driverPos = Offset(
                        x = (width * 0.2f) + ((index * 130) % (width * 0.6f).toInt()),
                        y = (height * 0.3f) + ((index * 90) % (height * 0.5f).toInt())
                    )

                    drawCircle(
                        color = YaVaYellowPrimary.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = driverPos
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 10.dp.toPx(),
                        center = driverPos
                    )
                    drawCircle(
                        color = YaVaYellowPrimary,
                        radius = 6.dp.toPx(),
                        center = driverPos
                    )
                }
            }

            // Overlay Badges & Map Controls
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(YaVaGreenSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zona Activa CDMX - Cobertura 100%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Map Zoom Controls
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
            ) {
                FloatingMapControl(
                    icon = Icons.Default.Add,
                    contentDesc = "Zoom in",
                    onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.5f) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                FloatingMapControl(
                    icon = Icons.Default.Remove,
                    contentDesc = "Zoom out",
                    onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.6f) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                FloatingMapControl(
                    icon = Icons.Default.MyLocation,
                    contentDesc = "Reset View",
                    onClick = { zoomScale = 1.0f }
                )
            }

            // Legend Overlay at Bottom Left
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
                    MapLegendItem(color = YaVaGreenSuccess, label = "Recolección")
                    Spacer(modifier = Modifier.width(8.dp))
                    MapLegendItem(color = YaVaRedAlert, label = "Entrega")
                    Spacer(modifier = Modifier.width(8.dp))
                    MapLegendItem(color = YaVaYellowPrimary, label = "Socio Conductores (${activeDrivers.size})")
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
