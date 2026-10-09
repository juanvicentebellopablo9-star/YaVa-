package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.YaVaYellowPrimary

@Composable
fun LegalConsentBox(
    modifier: Modifier = Modifier,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    privacyAccepted: Boolean,
    onPrivacyAcceptedChange: (Boolean) -> Unit,
    userRoleLabel: String = "Cliente / Conductor"
) {
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aceptación Digital de Términos y Privacidad (México)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Requisito legal para $userRoleLabel bajo normativa regulatoria de la LFPDPPP de México.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Terms Checkbox Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = onTermsAcceptedChange,
                    colors = CheckboxDefaults.colors(checkedColor = YaVaYellowPrimary, checkmarkColor = Color.Black),
                    modifier = Modifier.testTag("checkbox_accept_terms")
                )
                Text(
                    text = "Acepto los Términos y Condiciones",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.testTag("btn_view_terms")
                ) {
                    Text("Ver documento", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Privacy Checkbox Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = privacyAccepted,
                    onCheckedChange = onPrivacyAcceptedChange,
                    colors = CheckboxDefaults.colors(checkedColor = YaVaYellowPrimary, checkmarkColor = Color.Black),
                    modifier = Modifier.testTag("checkbox_accept_privacy")
                )
                Text(
                    text = "He leído el Aviso de Privacidad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.testTag("btn_view_privacy")
                ) {
                    Text("Ver documento", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (termsAccepted && privacyAccepted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aceptación registrada digitalmente (Versión v1.0-2026-MX)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyNoticeDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = YaVaYellowPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Términos y Condiciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "YaVa! Logística - Versión v1.0-2026-MX",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionTitle("1. Uso de la Plataforma")
                LegalBodyText("YaVa! conecta a clientes con socios conductores independientes para la recolección, transporte y entrega de paquetes y sobres en zonas urbanas autorizadas.")

                LegalSectionTitle("2. Responsabilidades del Cliente")
                LegalBodyText("El cliente se compromete a proporcionar direcciones exactas de origen y destino, datos de contacto verificables y empacar el envío de forma idónea para el tipo de carga seleccionado.")

                LegalSectionTitle("3. Responsabilidades del Socio Conductor")
                LegalBodyText("El socio conductor es responsable de mantener su vehículo en óptimas condiciones, cumplir con el reglamento de tránsito, resguardar el paquete durante el trayecto y registrar evidencia fotográfica o confirmación QR al entregar.")

                LegalSectionTitle("4. Reglas del Servicio y Tiempos de Espera")
                LegalBodyText("El tiempo de tolerancia máximo para recolección y entrega es de 10 minutos. Transcurrido este tiempo, el conductor podrá reprogramar o solicitar tarifa de tiempo de espera.")

                LegalSectionTitle("5. Cancelaciones y Penalizaciones")
                LegalBodyText("Las cancelaciones sin costo aplican únicamente antes de que el conductor inicie la ruta hacia el punto de recolección. Posterior a la asignación en camino, se aplicará un cargo mínimo de tarifa base.")

                LegalSectionTitle("6. Limitaciones de Responsabilidad")
                LegalBodyText("YaVa! actúa como plataforma tecnológica intermediaria. El valor máximo reembolsable por siniestro estándar está acotado según la cobertura solicitada o cotización contratada.")

                LegalSectionTitle("7. Objetos Restringidos y Prohibidos")
                LegalBodyText("Está estrictamente prohibido transportar: estupefacientes, sustancias tóxicas o inflamables, armas de fuego, explosivos, animales vivos, dinero en efectivo en montos no declarados o bienes ilícitos bajo leyes mexicanas.")

                LegalSectionTitle("8. Suspensión de Cuentas")
                LegalBodyText("Cualquier intento de fraude, violencia hacia conductores/clientes o violación de los presentes términos resultará en la suspensión inmediata e irrevocable de la cuenta.")

                LegalSectionTitle("9. Aceptación Digital")
                LegalBodyText("Al marcar la casilla correspondiente o solicitar un servicio en la plataforma, el usuario consiente digitalmente acatando el presente contrato marco de adhesión.")

                LegalSectionTitle("10. Titularidad, Propiedad Intelectual y Créditos de Desarrollo")
                LegalBodyText("El diseño, código fuente, algoritmos de cálculo, flujo de usuarios, arquitectura de software y dirección general de esta plataforma son creación original de Juan Vicente Bello Pablo, quien ostenta los cargos de Director, Planificador, Desarrollador, Ingeniero y Arquitecto de Software de YaVa! Express.")

                Spacer(modifier = Modifier.height(10.dp))

                var showTermsPdfDialog by remember { mutableStateOf(false) }

                if (showTermsPdfDialog) {
                    com.example.ui.components.TermsAndConditionsPdfDialog(
                        onDismiss = { showTermsPdfDialog = false }
                    )
                }

                OutlinedButton(
                    onClick = { showTermsPdfDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_legal_open_terms_pdf")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar PDF Términos y Condiciones (Nacional)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_close_terms")
                ) {
                    Text("Entendido y Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacyNoticeDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aviso de Privacidad (México)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LFPDPPP - YaVa! Envíos",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LegalSectionTitle("1. Datos Personales Recopilados")
                LegalBodyText("Recopilamos: Nombre completo, número telefónico, correo electrónico, direcciones de origen/destino, fotografía de perfil o evidencia de entrega, e información del vehículo en el caso de socios conductores.")

                LegalSectionTitle("2. Finalidad del Tratamiento de Datos")
                LegalBodyText("Sus datos se utilizan para: cotización de envíos, asignación de conductores, comunicación durante el pedido, generación de recibos, soporte técnico y mejora continua del servicio de logística.")

                LegalSectionTitle("3. Protección de Información")
                LegalBodyText("Implementamos medidas de seguridad físicas, técnicas y administrativas para resguardar sus datos contra pérdida, alteración, destrucción o uso no autorizado.")

                LegalSectionTitle("4. Uso de Ubicación GPS durante Servicios")
                LegalBodyText("Para asegurar la entrega y permitir el monitoreo en vivo en el mapa, la app recopila coordenadas GPS del socio conductor en primer y segundo plano durante los envíos activos.")

                LegalSectionTitle("5. Comunicación entre Cliente y Conductor")
                LegalBodyText("Las llamadas y chats se canalizan exclusivamente para fines del servicio activo sin compartir información personal innecesaria con terceros ajenos a la transacción.")

                LegalSectionTitle("6. Derechos ARCO (Acceso, Rectificación, Cancelación y Oposición)")
                LegalBodyText("Usted tiene derecho a ejercer sus derechos ARCO enviando una solicitud formal a nuestro correo oficial de privacidad: yavaenvios@gmail.com o al teléfono 999 743 1941.")

                LegalSectionTitle("7. Procedimiento para Eliminación de Datos")
                LegalBodyText("Para solicitar la baja permanente de sus datos personales o la eliminación de su cuenta, dirija su petición a yavaenvios@gmail.com con el asunto 'Solicitud de Baja de Datos ARCO'. Responderemos en un plazo máximo de 5 días hábiles.")

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_close_privacy")
                ) {
                    Text("Comprendido", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegalSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun LegalBodyText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 15.sp
    )
}
