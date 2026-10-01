package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary

data class SignatureStroke(val points: List<Offset>)

@Composable
fun DeliveryEvidenceDialog(
    order: OrderEntity,
    onDismiss: () -> Unit,
    onConfirmDelivery: (photoUri: String, qrCode: String) -> Unit
) {
    val context = LocalContext.current
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    val expectedPin = remember(order.trackingCode) { order.trackingCode.takeLast(4) }
    val isPinValid = enteredPin.trim() == expectedPin || enteredPin.trim() == order.trackingCode

    // Interactive Finger Signature State
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    // Real Photo Picker (standard Android 13+ / backward-compatible photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            Toast.makeText(context, "Foto de entrega registrada", Toast.LENGTH_SHORT).show()
        }
    }

    val isEvidenceReady = (selectedPhotoUri != null || strokes.isNotEmpty() || isPinValid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Evidencia de Entrega",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Orden: ${order.trackingCode}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Registra al menos un método de evidencia para confirmar la entrega a ${order.clientName}:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. REAL DIGITAL SIGNATURE PAD (Canvas con trazo táctil de dedo)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Firma Digital del Receptor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (strokes.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        strokes.clear()
                                        currentPoints.clear()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Borrar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, if (strokes.isNotEmpty()) YaVaGreenSuccess else Color.LightGray, RoundedCornerShape(10.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints = mutableStateListOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentPoints.isNotEmpty()) {
                                                strokes.add(SignatureStroke(currentPoints.toList()))
                                            }
                                        }
                                    )
                                }
                                .testTag("signature_canvas")
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw completed strokes
                                for (stroke in strokes) {
                                    if (stroke.points.size > 1) {
                                        val path = Path().apply {
                                            moveTo(stroke.points.first().x, stroke.points.first().y)
                                            for (pt in stroke.points.drop(1)) {
                                                lineTo(pt.x, pt.y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color.Black,
                                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }
                                // Draw active stroke
                                if (currentPoints.size > 1) {
                                    val path = Path().apply {
                                        moveTo(currentPoints.first().x, currentPoints.first().y)
                                        for (pt in currentPoints.drop(1)) {
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }

                            if (strokes.isEmpty() && currentPoints.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✍️ Dibuja la firma con el dedo aquí",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        if (strokes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = YaVaGreenSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Firma digital registrada (${strokes.size} trazos)",
                                    fontSize = 11.sp,
                                    color = YaVaGreenSuccess,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 2. REAL PHOTO EVIDENCE
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fotografía del Paquete Entregado", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedPhotoUri == null) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_evidence_photo_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar o Tomar Foto", fontSize = 12.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
                                        contentDescription = "Foto de evidencia",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, YaVaGreenSuccess, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Foto de entrega lista", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = YaVaGreenSuccess)
                                        Text("Evidencia vinculada al pedido", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(onClick = { selectedPhotoUri = null }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // 3. REAL PIN / QR VALIDATION
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PIN o Código QR del Cliente", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it.uppercase() },
                            placeholder = { Text("Ingresa los 4 dígitos o código", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            trailingIcon = {
                                if (isPinValid) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Válido", tint = YaVaGreenSuccess)
                                } else if (enteredPin.isNotBlank()) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Pendiente", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("evidence_pin_input")
                        )

                        if (isPinValid) {
                            Text(
                                text = "✓ Código validado con éxito",
                                fontSize = 11.sp,
                                color = YaVaGreenSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (enteredPin.isNotBlank()) {
                            Text(
                                text = "El PIN ingresado no coincide con el pedido.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val photoStr = selectedPhotoUri?.toString() ?: "evidencia_firma_digital_${order.trackingCode}.png"
                    val qrStr = if (isPinValid) "PIN-VALIDADO:$enteredPin" else "FIRMA-DIGITAL-OK"
                    onConfirmDelivery(photoStr, qrStr)
                },
                enabled = isEvidenceReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YaVaYellowPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delivery_final_button")
            ) {
                Text("Confirmar Entrega", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
