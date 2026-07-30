package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary

data class StatusStep(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val statusKey: String
)

@Composable
fun YaVaStatusTimeline(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        StatusStep("Creado", Icons.Default.NoteAdd, "Creado"),
        StatusStep("Buscando", Icons.Default.HourglassEmpty, "Esperando conductor"),
        StatusStep("Aceptado", Icons.Default.DirectionsBike, "Aceptado"),
        StatusStep("En camino", Icons.Default.LocalShipping, "En camino"),
        StatusStep("Entregado", Icons.Default.Check, "Entregado")
    )

    val currentStepIndex = when (currentStatus) {
        "Creado" -> 0
        "Esperando conductor" -> 1
        "Aceptado" -> 2
        "En camino" -> 3
        "Entregado" -> 4
        else -> 0
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado del Servicio:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentStatus.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentStatus == "Entregado") YaVaGreenSuccess else YaVaYellowPrimary,
                    modifier = Modifier
                        .background(
                            color = (if (currentStatus == "Entregado") YaVaGreenSuccess else YaVaYellowPrimary).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline graphic step dots with line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    val isDone = index <= currentStepIndex
                    val isCurrent = index == currentStepIndex

                    val circleColor by animateColorAsState(
                        targetValue = when {
                            isDone && step.statusKey == "Entregado" -> YaVaGreenSuccess
                            isDone -> YaVaYellowPrimary
                            else -> MaterialTheme.colorScheme.surface
                        },
                        label = "circleColor"
                    )

                    val contentColor = when {
                        isDone -> Color.Black
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = step.title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = step.title,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .weight(0.6f)
                                .background(
                                    if (index < currentStepIndex) YaVaYellowPrimary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }
    }
}
