package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TermsAndConditionsPdfHelper {

    fun generateTermsPdf(context: Context): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isFakeBoldText = true
            }
            val subTitlePaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 11f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.parseColor("#1565C0") // Deep Blue
                textSize = 11f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
            }
            val boldTextPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isFakeBoldText = true
            }
            val bannerPaint = Paint().apply {
                color = AndroidColor.parseColor("#E5A93B") // YaVa Yellow
                style = Paint.Style.FILL
            }

            // Header Banner
            canvas.drawRect(0f, 0f, 595f, 85f, bannerPaint)

            val bannerTextPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 16f
                isFakeBoldText = true
            }
            val bannerSubTextPaint = Paint().apply {
                color = AndroidColor.parseColor("#222222")
                textSize = 10f
                isFakeBoldText = true
            }

            canvas.drawText("TÉRMINOS Y CONDICIONES DE SERVICIO Y PRIVACIDAD", 25f, 38f, bannerTextPaint)
            canvas.drawText("Plataforma Digital Logística YaVa! Express — Mérida, Yucatán, México", 25f, 60f, bannerSubTextPaint)

            var y = 110f
            canvas.drawText("CONTRATO DE ADHESIÓN Y REGLAMENTO OPERATIVO DIGITAL", 25f, y, titlePaint)
            y += 18f

            val dateStr = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date())
            canvas.drawText("Vigencia y Actualización: $dateStr | Jurisdicción: Mérida, Yucatán, México", 25f, y, subTitlePaint)
            y += 20f

            canvas.drawLine(25f, y, 570f, y, Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1.2f })

            y += 20f
            canvas.drawText("1. DISPOSICIONES GENERALES Y MARCO LEGAL EN MÉXICO", 25f, y, headerPaint)
            y += 15f
            canvas.drawText("El presente documento constituye el Contrato de Adhesión para el uso de la plataforma digital YaVa! Express,", 25f, y, textPaint)
            y += 13f
            canvas.drawText("cumpliendo con la Ley Federal de Protección al Consumidor (PROFECO), el Código de Comercio de México,", 25f, y, textPaint)
            y += 13f
            canvas.drawText("y la Ley de Movilidad y Vialidad del Estado de Yucatán para servicios de intermediación de mensajería.", 25f, y, textPaint)

            y += 22f
            canvas.drawText("2. CREADOR, TITULAR Y DIRECCIÓN GENERAL", 25f, y, headerPaint)
            y += 15f
            canvas.drawText("• Titular y Creador Exclusivo: ", 25f, y, textPaint)
            canvas.drawText("Juan Vicente Bello Pablo", 170f, y, boldTextPaint)
            y += 13f
            canvas.drawText("• Cargos y Responsabilidad: ", 25f, y, textPaint)
            canvas.drawText("Director, Planificador, Desarrollador, Ingeniero y Arquitecto de Software.", 170f, y, textPaint)

            y += 22f
            canvas.drawText("3. DERECHOS Y OBLIGACIONES DEL USUARIO (CLIENTE)", 25f, y, headerPaint)
            y += 15f
            canvas.drawText("• Solicitar envíos con datos verídicos de origen, destino y contenido.", 25f, y, textPaint)
            y += 13f
            canvas.drawText("• Prohibición absoluta de enviar objetos ilícitos, explosivos, sustancias prohibidas o armas.", 25f, y, textPaint)
            y += 13f
            canvas.drawText("• Aceptar las tarifas calculadas por el algoritmo oficial conforme a distancia y volumen.", 25f, y, textPaint)

            y += 22f
            canvas.drawText("4. REGLAMENTO PARA SOCIOS CONDUCTORES Y REPARTIDORES", 25f, y, headerPaint)
            y += 15f
            canvas.drawText("• Contar con licencia de conducir vigente emitida en el Estado de Yucatán y vehículo en regla.", 25f, y, textPaint)
            y += 13f
            canvas.drawText("• Registrar evidencia digital obligatoria (fotografía / firma / código QR) al concretar la entrega.", 25f, y, textPaint)
            y += 13f
            canvas.drawText("• Cumplir con los lineamientos fiscales del SAT para retención de impuestos en plataformas digitales.", 25f, y, textPaint)

            y += 22f
            canvas.drawText("5. PROTECCIÓN DE DATOS PERSONALES (DERECHOS ARCO) Y JURISDICCIÓN", 25f, y, headerPaint)
            y += 15f
            canvas.drawText("• Sus datos personales están protegidos conforme a la LFPDPPP en México.", 25f, y, textPaint)
            y += 13f
            canvas.drawText("• Cualquier controversia legal se someterá expresamente a los Tribunales de la ciudad de Mérida, Yucatán.", 25f, y, textPaint)

            y += 28f
            canvas.drawLine(25f, y, 570f, y, Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1.2f })

            y += 30f
            val sealPaint = Paint().apply {
                color = AndroidColor.parseColor("#2E7D32")
                textSize = 11f
                isFakeBoldText = true
            }
            canvas.drawText("ACEPTACIÓN DIGITAL: DOCUMENTO VÁLIDO EN MÉXICO Y YUCATÁN", 25f, y, sealPaint)

            y += 35f
            val footerPaint = Paint().apply {
                color = AndroidColor.GRAY
                textSize = 8f
            }
            canvas.drawText("Documento legal de Términos de Servicio generado por YaVa! Express. Titularidad: Juan Vicente Bello Pablo.", 25f, y, footerPaint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Terminos_y_Condiciones_YaVa_Express_Merida.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generando PDF de Términos: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun shareTermsPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Términos y Condiciones de Servicio - YaVa! Express Mérida")
                putExtra(Intent.EXTRA_TEXT, "Adjunto documento oficial de Términos y Condiciones de Servicio y Privacidad para la Plataforma Digital YaVa! Express en Mérida, Yucatán.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Términos y Condiciones PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun TermsAndConditionsPdfDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_terms_pdf_view")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(YaVaYellowPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = ComposeColor.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NORMATIVA OFICIAL MÉXICO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Términos y Condiciones",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Surface(
                        color = YaVaGreenSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = YaVaGreenSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mérida, YUC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = YaVaGreenSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Reglamento Legal y Operativo (Mérida, Yucatán):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegalPoint("1. Marco Legal PROFECO", "Intermediación digital bajo leyes mexicanas y regulación comercial de Mérida, Yucatán.")
                    LegalPoint("2. Titular de Plataforma", "Juan Vicente Bello Pablo (Director, Planificador, Desarrollador y Arquitecto de Software).")
                    LegalPoint("3. Seguridad y Prohibiciones", "Estrictamente prohibido el traslado de objetos ilegales, armas o sustancias prohibidas.")
                    LegalPoint("4. Evidencia y Recepción", "Las entregas requieren validación con código QR, fotografía o firma digital.")
                    LegalPoint("5. Privacidad y Datos", "Protección de datos conforme a la Ley Federal de Protección de Datos Personales (ARCO).")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = TermsAndConditionsPdfHelper.generateTermsPdf(context)
                            if (file != null) {
                                TermsAndConditionsPdfHelper.shareTermsPdf(context, file)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = ComposeColor.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_terms_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargar PDF Términos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_close_terms_dialog")
                    ) {
                        Text("Cerrar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalPoint(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("• ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = YaVaYellowPrimary)
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
