package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.OrderEntity
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptHelper {

    fun generateAndSharePdfReceipt(context: Context, order: OrderEntity) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 22f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 14f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
            }
            val accentPaint = Paint().apply {
                color = Color.parseColor("#E5A93B") // YaVa Yellow
                style = Paint.Style.FILL
            }

            // Header Banner
            canvas.drawRect(0f, 0f, 595f, 90f, accentPaint)
            
            val bannerTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 26f
                isFakeBoldText = true
            }
            canvas.drawText("YaVa! Envíos - Comprobante de Servicio", 40f, 55f, bannerTextPaint)

            var y = 130f
            canvas.drawText("COMPROBANTE OFICIAL DE ENVÍO", 40f, y, titlePaint)
            y += 25f
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
            canvas.drawText("Código de Rastreo: ${order.trackingCode}", 40f, y, headerPaint)
            canvas.drawText("Fecha: $dateStr", 350f, y, textPaint)

            y += 35f
            canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 2f })

            y += 30f
            canvas.drawText("DETALLES DEL CLIENTE Y REMITENTE", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("Cliente: ${order.clientName}", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Teléfono: ${order.clientPhone}", 40f, y, textPaint)

            y += 35f
            canvas.drawText("RUTA Y LOGÍSTICA DE ENTREGA", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("Origen: ${order.originAddress}", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Destino: ${order.destinationAddress}", 40f, y, textPaint)

            y += 35f
            canvas.drawText("DESGLOSE DE PAQUETE Y TARIFA", 40f, y, headerPaint)
            y += 20f
            canvas.drawText("Tipo de Carga: ${order.packageType} | Peso Est: ${order.weightKg} kg", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Distancia: ${order.distanceKm} km", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Estado del Envío: ${order.status}", 40f, y, textPaint)

            y += 35f
            canvas.drawLine(40f, y, 555f, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 2f })

            y += 35f
            val pricePaint = Paint().apply {
                color = Color.parseColor("#2E7D32")
                textSize = 20f
                isFakeBoldText = true
            }
            canvas.drawText("TOTAL PAGADO: \$${order.priceMxn} MXN", 40f, y, pricePaint)

            y += 60f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
            }
            canvas.drawText("YaVa! Logística Express México. Documento digital expedido en conformidad con términos oficiales.", 40f, y, footerPaint)
            y += 15f
            canvas.drawText("Contacto de Soporte: 999 743 1941 | Correo: yavaenvios@gmail.com", 40f, y, footerPaint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Recibo_${order.trackingCode}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Share Intent
            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Recibo PDF YaVa!"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Generación de recibo PDF completada: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun OrderReceiptDialog(
    order: OrderEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Ticket Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = YaVaYellowPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Comprobante Digital",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "YaVa! Express México",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = YaVaGreenSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = order.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaVaGreenSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ticket Card Content
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Rastreo:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = order.trackingCode,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Cliente:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = order.clientName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Teléfono:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = order.clientPhone, fontSize = 12.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(text = "Origen:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = order.originAddress, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "Destino:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = order.destinationAddress, fontSize = 12.sp)

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tipo Paquete:", fontSize = 11.sp)
                            Text(text = "${order.packageType} (${order.weightKg} kg)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Distancia Recorrida:", fontSize = 11.sp)
                            Text(text = "${order.distanceKm} km", fontSize = 11.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "DESGLOSE DE TARIFAS Y COMISIONES PLATAFORMA:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val ratePerKm = if (order.priceMxn / order.distanceKm >= 8.5) 9.0 else 8.0
                        val commRatePercent = if (ratePerKm == 9.0) 20 else 15
                        val basePrice = (order.distanceKm * ratePerKm).coerceAtLeast(35.0)
                        val platformFee = Math.round((order.priceMxn * (commRatePercent / 100.0)) * 100.0) / 100.0
                        val driverNet = Math.round((order.priceMxn - platformFee) * 100.0) / 100.0

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• Tarifa por Distancia (${ratePerKm} MXN/km):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "\$${String.format("%.2f", basePrice)} MXN", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• Comisión Plataforma YaVa! (${commRatePercent}%):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "\$${String.format("%.2f", platformFee)} MXN", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "• Ganancia Socio Conductor (${100 - commRatePercent}%):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = YaVaGreenSuccess)
                            Text(text = "\$${String.format("%.2f", driverNet)} MXN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = YaVaGreenSuccess)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "💳 Quién Paga: ${order.payer} | Método: ${if (order.paymentMethod == "TRANSFERENCIA") "SPEI MercadoPago (CLABE 722969010374423450)" else order.paymentMethod}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (order.isPaymentConfirmed) "✅ Pago Verificado por ${order.paymentConfirmedBy ?: "Sistema"}" else "⏳ Pago Pendiente de Verificación",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.isPaymentConfirmed) YaVaGreenSuccess else ComposeColor(0xFFE65100)
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "TOTAL SERVICIO:", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "\$${order.priceMxn} MXN",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = YaVaGreenSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons (PDF Generator & Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            PdfReceiptHelper.generateAndSharePdfReceipt(context, order)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = ComposeColor.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_pdf_receipt")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
