package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.OrderEntity
import com.example.ui.components.DeliveryEvidenceDialog
import com.example.ui.components.GpsLocationHelper
import com.example.ui.components.LegalConsentBox
import com.example.ui.components.VoiceAssistantHelper
import com.example.ui.components.YaVaContactCard
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.YaVaViewModel

fun launchGoogleMapsNavigation(context: Context, address: String) {
    val encodedAddress = Uri.encode(address)
    val mapIntentUri = Uri.parse("google.navigation:q=$encodedAddress")
    val mapIntent = Intent(Intent.ACTION_VIEW, mapIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedAddress")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPortalScreen(
    viewModel: YaVaViewModel
) {
    val context = LocalContext.current
    val availableOrders by viewModel.availableOrders.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val approvedDrivers by viewModel.approvedDrivers.collectAsState()

    // Interactive Voice Assistant
    val voiceAssistant = remember { VoiceAssistantHelper(context) }
    var isVoiceEnabled by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            voiceAssistant.shutdown()
        }
    }

    var driverGpsAddress by remember { mutableStateOf("Coordenadas GPS no capturadas") }
    var isFetchingGps by remember { mutableStateOf(false) }

    var showRegistrationForm by remember { mutableStateOf(false) }

    // Registration state
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val vehicleTypes = listOf("Motocicleta", "Auto Sedan", "Camioneta Ligera", "Bicicleta Eléctrica")
    var selectedVehicle by remember { mutableStateOf(vehicleTypes[0]) }
    var expandedVehicle by remember { mutableStateOf(false) }

    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("2024") }
    var licensePlate by remember { mutableStateOf("") }
    var cargoCapacityStr by remember { mutableStateOf("25.0") }

    val zones = listOf("Centro", "Norte", "Sur", "Oriente", "Poniente")
    var selectedZone by remember { mutableStateOf(zones[0]) }
    var expandedZone by remember { mutableStateOf(false) }

    var availableSchedule by remember { mutableStateOf("Tiempo Completo (8:00 - 20:00)") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }

    var termsAccepted by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }

    // Evidence Dialog state
    var evidenceOrderTarget by remember { mutableStateOf<OrderEntity?>(null) }

    val currentDriver = approvedDrivers.firstOrNull() ?: DriverEntity(
        id = 1,
        fullName = "Carlos Mendoza",
        phone = "+52 55 1234 5678",
        vehicle = "Motocicleta Electric 2024",
        zone = "Centro",
        isApproved = true,
        isAvailable = true,
        rating = 4.9f,
        totalDeliveries = 142
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Driver Header Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = YaVaYellowPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Portal Socio YaVa!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = YaVaGreenSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVO & DISPONIBLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = YaVaGreenSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bienvenido, ${currentDriver.fullName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vehículo: ${currentDriver.vehicle} | Zona: ${currentDriver.zone} | Entregas: ${currentDriver.totalDeliveries}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Voice Assistant Control
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = YaVaYellowPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Asistente de Voz Cálida",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Guía por voz paso a paso en cada servicio",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    voiceAssistant.speak("Hola socio $currentDriver.fullName. Asistente de voz YaVa activado. Te asistiré durante todo el trayecto de tu servicio.")
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_test_voice")
                            ) {
                                Text("Probar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isVoiceEnabled,
                                onCheckedChange = { isVoiceEnabled = it },
                                modifier = Modifier.testTag("switch_voice_assistant")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // GPS Driver Location Accuracy Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Precisión GPS Real del Socio:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = driverGpsAddress,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isFetchingGps = true
                            GpsLocationHelper.getCurrentRealGpsLocation(
                                context = context,
                                onLocationReceived = { gpsLoc ->
                                    isFetchingGps = false
                                    driverGpsAddress = gpsLoc.formattedAddress
                                    Toast.makeText(context, "GPS Socio Actualizado: ${gpsLoc.formattedAddress}", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isFetchingGps = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_fetch_driver_gps")
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFetchingGps) "Obteniendo..." else "Actualizar GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showRegistrationForm = !showRegistrationForm },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.testTag("toggle_driver_registration_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showRegistrationForm) "Ocultar Formulario" else "Solicitar Registro de Conductor",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Driver Registration Form
        if (showRegistrationForm) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Registro Profesional de Socio Conductor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresa tus datos completos para validación administrativa y alta en la red YaVa!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. DATOS PERSONALES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre Completo *") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_reg_name")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de Contacto WhatsApp *") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_reg_phone")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico *") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_reg_email")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("2. DATOS DEL VEHÍCULO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    ExposedDropdownMenuBox(
                        expanded = expandedVehicle,
                        onExpandedChange = { expandedVehicle = !expandedVehicle }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Vehículo *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicle,
                            onDismissRequest = { expandedVehicle = false }
                        ) {
                            vehicleTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedVehicle = type
                                        expandedVehicle = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_brand")
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Modelo") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_model")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { yearStr = it },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_year")
                        )
                        OutlinedTextField(
                            value = licensePlate,
                            onValueChange = { licensePlate = it },
                            label = { Text("Placas") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_plate")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cargoCapacityStr,
                        onValueChange = { cargoCapacityStr = it },
                        label = { Text("Capacidad Máxima Carga (kg)") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_reg_capacity")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("3. COBERTURA Y HORARIO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    ExposedDropdownMenuBox(
                        expanded = expandedZone,
                        onExpandedChange = { expandedZone = !expandedZone }
                    ) {
                        OutlinedTextField(
                            value = selectedZone,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Zona de Cobertura Principal *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedZone) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedZone,
                            onDismissRequest = { expandedZone = false }
                        ) {
                            zones.forEach { z ->
                                DropdownMenuItem(
                                    text = { Text(z) },
                                    onClick = {
                                        selectedZone = z
                                        expandedZone = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = availableSchedule,
                        onValueChange = { availableSchedule = it },
                        label = { Text("Horario Disponible") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_reg_schedule")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("4. CONTACTO DE EMERGENCIA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emergencyContactName,
                            onValueChange = { emergencyContactName = it },
                            label = { Text("Nombre Contacto") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_emerg_name")
                        )
                        OutlinedTextField(
                            value = emergencyContactPhone,
                            onValueChange = { emergencyContactPhone = it },
                            label = { Text("Teléfono") },
                            modifier = Modifier.weight(1f).testTag("driver_reg_emerg_phone")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LegalConsentBox(
                        termsAccepted = termsAccepted,
                        onTermsAcceptedChange = { termsAccepted = it },
                        privacyAccepted = privacyAccepted,
                        onPrivacyAcceptedChange = { privacyAccepted = it },
                        userRoleLabel = "Socio Conductor YaVa!"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val yearVal = yearStr.toIntOrNull() ?: 2024
                            val capacityVal = cargoCapacityStr.toDoubleOrNull() ?: 25.0
                            viewModel.registerDriverApplication(
                                fullName = fullName,
                                phone = phone,
                                vehicle = "$selectedVehicle $brand $model",
                                zone = selectedZone,
                                email = email,
                                brand = brand,
                                model = model,
                                year = yearVal,
                                licensePlate = licensePlate,
                                cargoCapacityKg = capacityVal,
                                coverageZone = selectedZone,
                                availableSchedule = availableSchedule,
                                emergencyContactName = emergencyContactName,
                                emergencyContactPhone = emergencyContactPhone,
                                termsAccepted = termsAccepted,
                                privacyAccepted = privacyAccepted
                            )
                            showRegistrationForm = false
                        },
                        enabled = fullName.isNotBlank() && phone.isNotBlank() && termsAccepted && privacyAccepted,
                        colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_driver_registration_button")
                    ) {
                        Text("Enviar Solicitud Profesional de Socio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Available Orders to Accept
        Text(
            text = "Envíos Disponibles para Aceptar (${availableOrders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (availableOrders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No hay envíos pendientes en espera en este momento.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                availableOrders.forEach { order ->
                    AvailableOrderCard(
                        order = order,
                        onAccept = {
                            viewModel.acceptOrderByDriver(order.id, currentDriver)
                            if (isVoiceEnabled) {
                                voiceAssistant.speakServiceAccepted(order.trackingCode, order.originAddress)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Active Driver Assigned Deliveries Control
        val activeAssignedOrders = allOrders.filter { it.driverId == currentDriver.id && it.status != "Entregado" }

        Text(
            text = "Tus Servicios Asignados Activos (${activeAssignedOrders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (activeAssignedOrders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No tienes entregas en progreso actualmente. Acepta un pedido de la lista superior.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeAssignedOrders.forEach { order ->
                    ActiveDriverDeliveryCard(
                        order = order,
                        onAdvanceStatus = {
                            if (order.status == "En camino") {
                                evidenceOrderTarget = order
                            } else {
                                viewModel.advanceOrderStatus(order.id, order.status)
                                if (isVoiceEnabled) {
                                    voiceAssistant.speakPackageCollected(order.destinationAddress)
                                }
                            }
                        },
                        onConfirmPayment = { orderId ->
                            viewModel.confirmPaymentByDriver(orderId)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        YaVaContactCard(
            customMessage = "Hola YaVa!, soy Socio Conductor y necesito soporte operativo.",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Evidence Dialog Modal
    if (evidenceOrderTarget != null) {
        DeliveryEvidenceDialog(
            order = evidenceOrderTarget!!,
            onDismiss = { evidenceOrderTarget = null },
            onConfirmDelivery = { photoUri, qrCode ->
                val targetOrder = evidenceOrderTarget!!
                viewModel.completeDeliveryWithProof(targetOrder.id, photoUri, qrCode)
                if (isVoiceEnabled) {
                    voiceAssistant.speakDeliveryCompleted(targetOrder.trackingCode, targetOrder.priceMxn)
                }
                evidenceOrderTarget = null
            }
        )
    }
}

@Composable
private fun AvailableOrderCard(
    order: OrderEntity,
    onAccept: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.trackingCode,
                    fontWeight = FontWeight.ExtraBold,
                    color = YaVaYellowPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = "\$${order.priceMxn} MXN",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Origen: ${order.originAddress}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Destino: ${order.destinationAddress}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paquete: ${order.packageType} (${order.weightKg} kg) | Distancia: ${order.distanceKm} km",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { launchGoogleMapsNavigation(context, order.destinationAddress) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_route_gmaps_${order.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver Ruta en Google Maps / Navegar Destino", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YaVaYellowPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("accept_order_${order.id}_button")
            ) {
                Text("Aceptar Servicio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveDriverDeliveryCard(
    order: OrderEntity,
    onAdvanceStatus: () -> Unit,
    onConfirmPayment: (Long) -> Unit
) {
    val context = LocalContext.current
    val paymentMethodLabel = when (order.paymentMethod) {
        "TRANSFERENCIA" -> "Transferencia SPEI (MercadoPago)"
        "TERMINAL" -> "Cobro con Tarjeta (Terminal)"
        else -> "Efectivo al Conductor"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = order.trackingCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = order.status.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    color = YaVaYellowPrimary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Cliente: ${order.clientName} (${order.clientPhone})", fontSize = 12.sp)
            Text(text = "Origen: ${order.originAddress}", fontSize = 12.sp)
            Text(text = "Destino: ${order.destinationAddress}", fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            // Payment & Accounting Info for Driver
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "💳 Condiciones de Cobro:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Quién Paga: ${order.payer}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Método: $paymentMethodLabel | Total: \$${order.priceMxn} MXN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (order.isPaymentConfirmed) {
                        Text(
                            text = "✅ Pago Confirmado (por ${order.paymentConfirmedBy ?: "Director/Sistema"})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaVaGreenSuccess
                        )
                    } else if (order.paymentMethod == "TRANSFERENCIA") {
                        Text(
                            text = "⏳ Transferencia SPEI registrada. El Director verificará la recepción en la cuenta MercadoPago.",
                            fontSize = 10.sp,
                            color = Color(0xFFE65100)
                        )
                    } else {
                        // Cash or Terminal - Driver must confirm collection
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onConfirmPayment(order.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YaVaGreenSuccess,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_confirm_payment_${order.id}")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirmar Cobro Recibido (${order.paymentMethod})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation buttons to Google Maps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { launchGoogleMapsNavigation(context, order.originAddress) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_origin_gmaps_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navegar Origen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { launchGoogleMapsNavigation(context, order.destinationAddress) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4), // Google Blue
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_destination_gmaps_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navegar Destino", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val nextButtonLabel = when (order.status) {
                "Aceptado" -> "Iniciar Recolección -> En Camino"
                "En camino" -> "Completar Entrega y Registrar Evidencia"
                else -> "Actualizar Estado"
            }

            Button(
                onClick = onAdvanceStatus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (order.status == "En camino") YaVaGreenSuccess else MaterialTheme.colorScheme.primary,
                    contentColor = if (order.status == "En camino") Color.White else Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("advance_order_${order.id}_button")
            ) {
                Text(nextButtonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
