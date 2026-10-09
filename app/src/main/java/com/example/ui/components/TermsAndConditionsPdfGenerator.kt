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
            val pageWidth = 595
            val pageHeight = 842
            val leftMargin = 25f
            val rightMargin = 570f
            val topMargin = 25f
            val bottomMargin = 800f
            var pageNumber = 1

            // Paints
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK; textSize = 15f; isFakeBoldText = true
            }
            val subTitlePaint = Paint().apply {
                color = AndroidColor.DKGRAY; textSize = 11f; isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.parseColor("#1565C0"); textSize = 11f; isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK; textSize = 9.5f
            }
            val boldTextPaint = Paint().apply {
                color = AndroidColor.BLACK; textSize = 9.5f; isFakeBoldText = true
            }
            val bannerPaint = Paint().apply {
                color = AndroidColor.parseColor("#E5A93B"); style = Paint.Style.FILL
            }
            val bannerTextPaint = Paint().apply {
                color = AndroidColor.BLACK; textSize = 16f; isFakeBoldText = true
            }
            val bannerSubTextPaint = Paint().apply {
                color = AndroidColor.parseColor("#222222"); textSize = 10f; isFakeBoldText = true
            }
            val sealPaint = Paint().apply {
                color = AndroidColor.parseColor("#2E7D32"); textSize = 11f; isFakeBoldText = true
            }
            val footerPaint = Paint().apply {
                color = AndroidColor.GRAY; textSize = 8f
            }
            val dividerPaint = Paint().apply {
                color = AndroidColor.LTGRAY; strokeWidth = 1.2f
            }

            // --- Pagination helper ---
            var canvas: Canvas
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas

            fun newPage() {
                // Footer on current page before turning
                canvas.drawText(
                    "YaVa! Express — Términos y Condiciones Nacionales | Página $pageNumber",
                    leftMargin, pageHeight - 20f, footerPaint
                )
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
            }

            fun ensureSpace(needed: Float, y: Float): Float {
                if (y + needed > bottomMargin) {
                    newPage()
                    return topMargin
                }
                return y
            }

            fun drawHeader(title: String, y: Float): Float {
                var yy = ensureSpace(20f, y)
                canvas.drawText(title, leftMargin, yy, headerPaint)
                yy += 15f
                return yy
            }

            fun drawWrappedText(text: String, y: Float, paint: Paint = textPaint, maxWidth: Float = 545f): Float {
                val words = text.split(" ")
                val sb = StringBuilder()
                var yy = y
                for (word in words) {
                    val testLine = if (sb.isEmpty()) word else "$sb $word"
                    if (paint.measureText(testLine) > maxWidth) {
                        yy = ensureSpace(13f, yy)
                        canvas.drawText(sb.toString(), leftMargin, yy, paint)
                        yy += 13f
                        sb.clear()
                        sb.append(word)
                    } else {
                        sb.clear()
                        sb.append(testLine)
                    }
                }
                if (sb.isNotEmpty()) {
                    yy = ensureSpace(13f, yy)
                    canvas.drawText(sb.toString(), leftMargin, yy, paint)
                    yy += 13f
                }
                return yy
            }

            fun drawBullet(label: String, text: String, y: Float): Float {
                val yy = ensureSpace(13f, y)
                canvas.drawText("• $label", leftMargin, yy, textPaint)
                val labelWidth = textPaint.measureText("• $label  ")
                // Wrap the continuation text after the label
                val words = text.split(" ")
                val sb = StringBuilder()
                var lineY = yy
                var firstLine = true
                for (word in words) {
                    val testLine = if (sb.isEmpty()) word else "$sb $word"
                    val prefixWidth = if (firstLine) labelWidth else 0f
                    if (paint_measureWithPrefix(textPaint, testLine, prefixWidth) > 545f) {
                        canvas.drawText(sb.toString(), leftMargin + prefixWidth, lineY, textPaint)
                        lineY += 13f
                        lineY = ensureSpace(13f, lineY)
                        sb.clear()
                        sb.append(word)
                        firstLine = false
                    } else {
                        sb.clear()
                        sb.append(testLine)
                    }
                }
                if (sb.isNotEmpty()) {
                    val prefixWidth = if (firstLine) labelWidth else 0f
                    canvas.drawText(sb.toString(), leftMargin + prefixWidth, lineY, textPaint)
                    lineY += 13f
                }
                return lineY
            }

            // --- Page 1: Header Banner ---
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 85f, bannerPaint)
            canvas.drawText("TÉRMINOS Y CONDICIONES DE SERVICIO Y PRIVACIDAD", 25f, 38f, bannerTextPaint)
            canvas.drawText("Plataforma Digital Logística YaVa! Express — Cobertura Nacional, México", 25f, 60f, bannerSubTextPaint)

            var y = 110f
            canvas.drawText("CONTRATO DE ADHESIÓN Y REGLAMENTO OPERATIVO DIGITAL", leftMargin, y, titlePaint)
            y += 18f

            val dateStr = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date())
            canvas.drawText("Vigencia y Actualización: $dateStr | Jurisdicción: República Mexicana", leftMargin, y, subTitlePaint)
            y += 20f
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 20f

            // --- Section 1 ---
            y = drawHeader("1. DISPOSICIONES GENERALES Y MARCO LEGAL EN MÉXICO", y)
            y = drawWrappedText(
                "El presente documento constituye el Contrato de Adhesión para el uso de la plataforma digital YaVa! Express, " +
                "cumpliendo con la Ley Federal de Protección al Consumidor (PROFECO), el Código de Comercio de México, " +
                "y las leyes de movilidad y vialidad aplicables en toda la República Mexicana para servicios de intermediación de mensajería.",
                y
            )
            y += 9f

            // --- Section 2 ---
            y = drawHeader("2. CREADOR, TITULAR Y DIRECCIÓN GENERAL", y)
            y = ensureSpace(26f, y)
            canvas.drawText("• Titular y Creador Exclusivo: ", leftMargin, y, textPaint)
            canvas.drawText("Juan Vicente Bello Pablo", 170f, y, boldTextPaint)
            y += 13f
            y = ensureSpace(13f, y)
            canvas.drawText("• Cargos y Responsabilidad: ", leftMargin, y, textPaint)
            canvas.drawText("Director, Planificador, Desarrollador, Ingeniero y Arquitecto de Software.", 170f, y, textPaint)
            y += 22f

            // --- Section 3 ---
            y = drawHeader("3. DERECHOS Y OBLIGACIONES DEL USUARIO (CLIENTE)", y)
            y = drawWrappedText("• Solicitar envíos con datos verídicos de origen, destino y contenido en cualquier estado de la República.", y)
            y = drawWrappedText("• Prohibición absoluta de enviar objetos ilícitos, explosivos, sustancias prohibidas o armas.", y)
            y = drawWrappedText("• Aceptar las tarifas calculadas por el algoritmo oficial conforme a distancia, volumen y nivel de servicio.", y)
            y += 9f

            // --- Section 4 ---
            y = drawHeader("4. REGLAMENTO PARA SOCIOS CONDUCTORES Y REPARTIDORES", y)
            y = drawWrappedText("• Contar con licencia de conducir vigente emitida en cualquier estado de la República y vehículo en regla.", y)
            y = drawWrappedText("• Registrar evidencia digital obligatoria (fotografía / firma / código QR) al concretar la entrega.", y)
            y = drawWrappedText("• Cumplir con los lineamientos fiscales del SAT para retención de impuestos en plataformas digitales.", y)
            y += 9f

            // --- Section 5 ---
            y = drawHeader("5. REGLAS DEL SERVICIO Y TIEMPOS DE ESPERA", y)
            y = drawWrappedText("El tiempo de tolerancia máximo para recolección y entrega es de 10 minutos. Transcurrido este tiempo, el conductor podrá reprogramar o solicitar tarifa de tiempo de espera.", y)
            y += 9f

            // --- Section 6 ---
            y = drawHeader("6. CANCELACIONES Y PENALIZACIONES", y)
            y = drawWrappedText("Las cancelaciones sin costo aplican únicamente antes de que el conductor inicie la ruta hacia el punto de recolección. Posterior a la asignación en camino, se aplicará un cargo mínimo de tarifa base.", y)
            y += 9f

            // --- Section 7 ---
            y = drawHeader("7. LIMITACIONES DE RESPONSABILIDAD", y)
            y = drawWrappedText("YaVa! actúa como plataforma tecnológica intermediaria. El valor máximo reembolsable por siniestro estándar está acotado según la cobertura solicitada o cotización contratada.", y)
            y += 9f

            // --- Section 8 ---
            y = drawHeader("8. OBJETOS RESTRINGIDOS Y PROHIBIDOS", y)
            y = drawWrappedText("Está estrictamente prohibido transportar: estupefacientes, sustancias tóxicas o inflamables, armas de fuego, explosivos, animales vivos, dinero en efectivo en montos no declarados o bienes ilícitos bajo leyes mexicanas.", y)
            y += 9f

            // --- Section 9 ---
            y = drawHeader("9. SUSPENSIÓN DE CUENTAS", y)
            y = drawWrappedText("Cualquier intento de fraude, violencia hacia conductores/clientes o violación de los presentes términos resultará en la suspensión inmediata e irrevocable de la cuenta.", y)
            y += 9f

            // --- Section 10 ---
            y = drawHeader("10. PROTECCIÓN DE DATOS PERSONALES (DERECHOS ARCO) Y JURISDICCIÓN", y)
            y = drawWrappedText("• Sus datos personales están protegidos conforme a la LFPDPPP en México.", y)
            y = drawWrappedText("• Derechos ARCO: acceso, rectificación, cancelación y oposición mediante solicitud a yavaenvios@gmail.com.", y)
            y = drawWrappedText("• Cualquier controversia legal se someterá expresamente a los Tribunales competentes de la República Mexicana.", y)
            y += 9f

            // --- Section 11 ---
            y = drawHeader("11. COBERTURA NACIONAL Y NIVELES DE SERVICIO", y)
            y = drawWrappedText(
                "YaVa! Express opera con cobertura en los 32 estados de la República Mexicana, ofreciendo tres niveles de servicio: " +
                "Envío Local Urbano (dentro de la misma ciudad), Envío Intercity (entre ciudades del mismo estado o estados cercanos) " +
                "y Envío Nacional de larga distancia (entre cualquier estado de la República). Las tarifas se calculan dinámicamente " +
                "según distancia, tiempo estimado de trayecto y nivel de servicio seleccionado.",
                y
            )
            y += 9f

            // --- Section 12 ---
            y = drawHeader("12. TITULARIDAD, PROPIEDAD INTELECTUAL Y CRÉDITOS DE DESARROLLO", y)
            y = drawWrappedText(
                "El diseño, código fuente, algoritmos de cálculo, flujo de usuarios, arquitectura de software y dirección general " +
                "de esta plataforma son creación original de Juan Vicente Bello Pablo, quien ostenta los cargos de Director, " +
                "Planificador, Desarrollador, Ingeniero y Arquitecto de Software de YaVa! Express.",
                y
            )
            y += 9f

            // --- Section 13 ---
            y = drawHeader("13. ACEPTACIÓN DIGITAL", y)
            y = drawWrappedText(
                "Al marcar la casilla correspondiente o solicitar un servicio en la plataforma, el usuario consiente digitalmente " +
                "acatando el presente contrato marco de adhesión, válido en toda la República Mexicana.",
                y
            )

            // --- Seal & Footer ---
            y += 18f
            y = ensureSpace(60f, y)
            canvas.drawLine(leftMargin, y, rightMargin, y, dividerPaint)
            y += 20f
            canvas.drawText("ACEPTACIÓN DIGITAL: DOCUMENTO VÁLIDO EN LA REPÚBLICA MEXICANA", leftMargin, y, sealPaint)
            y += 20f
            canvas.drawText("Versión v1.0-2026-MX | Generado: $dateStr", leftMargin, y, subTitlePaint)

            // Final page footer
            canvas.drawText(
                "YaVa! Express — Términos y Condiciones Nacionales | Página $pageNumber",
                leftMargin, pageHeight - 20f, footerPaint
            )

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Terminos_y_Condiciones_YaVa_Express_Nacional.pdf")
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

    private fun paint_measureWithPrefix(paint: Paint, text: String, prefixWidth: Float): Float {
        return prefixWidth + paint.measureText(text)
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
                putExtra(Intent.EXTRA_SUBJECT, "Términos y Condiciones de Servicio - YaVa! Express Nacional")
                putExtra(Intent.EXTRA_TEXT, "Adjunto documento oficial de Términos y Condiciones de Servicio y Privacidad para la Plataforma Digital YaVa! Express con cobertura nacional en la República Mexicana.")
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
                                text = "República Mexicana",
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
                    text = "Reglamento Legal y Operativo (República Mexicana):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegalPoint("1. Marco Legal PROFECO", "Intermediación digital bajo leyes mexicanas y regulación comercial aplicable en toda la República Mexicana.")
                    LegalPoint("2. Titular de Plataforma", "Juan Vicente Bello Pablo (Director, Planificador, Desarrollador, Ingeniero y Arquitecto de Software).")
                    LegalPoint("3. Derechos del Cliente", "Datos verídicos de origen, destino y contenido; aceptación de tarifas calculadas por el algoritmo oficial.")
                    LegalPoint("4. Reglamento de Conductores", "Licencia vigente de cualquier estado, evidencia digital obligatoria y cumplimiento fiscal SAT.")
                    LegalPoint("5. Tiempos de Espera", "Tolerancia máxima de 10 minutos para recolección y entrega; posterior aplica tarifa de espera.")
                    LegalPoint("6. Cancelaciones", "Sin costo antes de que el conductor inicie ruta; posterior se aplica cargo mínimo de tarifa base.")
                    LegalPoint("7. Limitación de Responsabilidad", "YaVa! es intermediario tecnológico; reembolso acotado según cobertura o cotización contratada.")
                    LegalPoint("8. Objetos Prohibidos", "Estupefacientes, tóxicos, inflamables, armas, explosivos, animales vivos, efectivo no declarado, bienes ilícitos.")
                    LegalPoint("9. Suspensión de Cuentas", "Fraude, violencia o violación de términos resulta en suspensión inmediata e irrevocable.")
                    LegalPoint("10. Privacidad y Datos ARCO", "Protección conforme a LFPDPPP; derechos ARCO mediante solicitud a yavaenvios@gmail.com.")
                    LegalPoint("11. Cobertura Nacional", "32 estados, 3 niveles de servicio: Local Urbano, Intercity y Nacional de larga distancia.")
                    LegalPoint("12. Propiedad Intelectual", "Código, algoritmos y arquitectura son creación original de Juan Vicente Bello Pablo.")
                    LegalPoint("13. Aceptación Digital", "Al marcar la casilla o solicitar servicio, el usuario consiente este contrato válido en toda la República.")
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
