package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Thunderstorm
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PricingCalculator
import com.example.ui.components.GpsLocationHelper
import com.example.ui.components.YaVaContactCard
import com.example.ui.theme.YaVaGreenSuccess
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.YaVaViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.components.AddressSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRequestScreen(
    viewModel: YaVaViewModel,
    onOrderCreatedAndTrack: () -> Unit
) {
    val context = LocalContext.current

    var clientName by remember { mutableStateOf("Ana Paula Martínez") }
    var clientPhone by remember { mutableStateOf("+52 55 9876 5432") }
    var clientEmail by remember { mutableStateOf("anapaula@ejemplo.com") }
    var originAddress by remember { mutableStateOf("Paseo de Montejo, Centro, Mérida, Yucatán") }
    var destinationAddress by remember { mutableStateOf("Calle 7 No. 451, Altabrisa, Mérida, Yucatán") }

    var originLat by remember { mutableDoubleStateOf(20.9850) }
    var originLng by remember { mutableDoubleStateOf(-89.6180) }
    var destLat by remember { mutableDoubleStateOf(21.0188) }
    var destLng by remember { mutableDoubleStateOf(-89.5840) }

    var originSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var destSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var showOriginSuggestions by remember { mutableStateOf(false) }
    var showDestSuggestions by remember { mutableStateOf(false) }

    var isCalculatingDistance by remember { mutableStateOf(false) }

    val packageTypes = listOf(
        "Documentos",
        "Paquete Pequeño (< 3 kg)",
        "Paquete Mediano (3 - 10 kg)",
        "Paquete Pesado (hasta 20 kg)"
    )
    var selectedType by remember { mutableStateOf(packageTypes[1]) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var weightKg by remember { mutableDoubleStateOf(3.5) }
    var distanceKm by remember { mutableDoubleStateOf(6.2) }
    var notes by remember { mutableStateOf("Entregar en portón negro de 9:00 a 18:00 h.") }

    var isFetchingGps by remember { mutableStateOf(false) }

    // Helper to calculate distance based on real addresses and coordinates
    fun triggerRealDistanceCalculation() {
        isCalculatingDistance = true
        GpsLocationHelper.calculateRealDistanceKm(
            context = context,
            originAddress = originAddress,
            destAddress = destinationAddress,
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng
        ) { calculatedKm, _, oLat, oLng, dLat, dLng ->
            isCalculatingDistance = false
            distanceKm = calculatedKm
            originLat = oLat
            originLng = oLng
            destLat = dLat
            destLng = dLng
        }
    }

    LaunchedEffect(originAddress) {
        if (originAddress.length >= 2 && showOriginSuggestions) {
            GpsLocationHelper.searchAddressSuggestions(context, originAddress) { suggestions ->
                originSuggestions = suggestions
            }
        } else {
            originSuggestions = emptyList()
        }
    }

    LaunchedEffect(destinationAddress) {
        if (destinationAddress.length >= 2 && showDestSuggestions) {
            GpsLocationHelper.searchAddressSuggestions(context, destinationAddress) { suggestions ->
                destSuggestions = suggestions
            }
        } else {
            destSuggestions = emptyList()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            isFetchingGps = true
            GpsLocationHelper.getCurrentRealGpsLocation(
                context = context,
                onLocationReceived = { gpsLoc ->
                    isFetchingGps = false
                    originAddress = gpsLoc.formattedAddress
                    originLat = gpsLoc.latitude
                    originLng = gpsLoc.longitude
                    showOriginSuggestions = false
                    triggerRealDistanceCalculation()
                    Toast.makeText(context, "GPS Obtenido: ${gpsLoc.formattedAddress}", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingGps = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            Toast.makeText(context, "Permiso de GPS denegado por el usuario.", Toast.LENGTH_SHORT).show()
        }
    }

    // Payment method & payer selection
    val payerOptions = listOf("Paga quien envía", "Paga quien recibe")
    var selectedPayer by remember { mutableStateOf(payerOptions[0]) }

    val paymentMethodOptions = listOf(
        "EFECTIVO" to "Efectivo al Conductor",
        "TERMINAL" to "Cobro con Tarjeta (Terminal)",
        "TRANSFERENCIA" to "Transferencia SPEI (MercadoPago)"
    )
    var selectedPaymentMethod by remember { mutableStateOf("EFECTIVO") }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val quote = PricingCalculator.calculateQuote(
        distanceKm = distanceKm
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Banner Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Solicitud Directa de Envío Local",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Procesamiento inmediato de tu paquete con Socio Conductor YaVa! activo.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Client & Address Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Datos del Cliente Remitente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Teléfono de Contacto") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_phone")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ubicación y Direcciones GPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            if (GpsLocationHelper.hasLocationPermission(context)) {
                                isFetchingGps = true
                                GpsLocationHelper.getCurrentRealGpsLocation(
                                    context = context,
                                    onLocationReceived = { gpsLoc ->
                                        isFetchingGps = false
                                        originAddress = gpsLoc.formattedAddress
                                        Toast.makeText(context, "GPS Obtenido: ${gpsLoc.formattedAddress}", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        isFetchingGps = false
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_fetch_client_gps")
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFetchingGps) "Obteniendo GPS..." else "Usar GPS Actual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Origin Address with Autocomplete Suggestions
                Column {
                    OutlinedTextField(
                        value = originAddress,
                        onValueChange = {
                            originAddress = it
                            showOriginSuggestions = true
                        },
                        label = { Text("Dirección de Origen (Recolección)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF34C759)) },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar ubicación", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_origin_address")
                    )

                    if (showOriginSuggestions && originSuggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "💡 Ubicaciones sugeridas:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                originSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                originAddress = suggestion.fullAddress
                                                originLat = suggestion.latitude
                                                originLng = suggestion.longitude
                                                showOriginSuggestions = false
                                                triggerRealDistanceCalculation()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF34C759),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = suggestion.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = suggestion.fullAddress,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Destination Address with Autocomplete Suggestions
                Column {
                    OutlinedTextField(
                        value = destinationAddress,
                        onValueChange = {
                            destinationAddress = it
                            showDestSuggestions = true
                        },
                        label = { Text("Dirección de Destino (Entrega)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF3B30)) },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar ubicación", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_destination_address")
                    )

                    if (showDestSuggestions && destSuggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "💡 Ubicaciones sugeridas:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                destSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                destinationAddress = suggestion.fullAddress
                                                destLat = suggestion.latitude
                                                destLng = suggestion.longitude
                                                showDestSuggestions = false
                                                triggerRealDistanceCalculation()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFFFF3B30),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = suggestion.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = suggestion.fullAddress,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Button to explicitly re-calculate real GPS route distance
                OutlinedButton(
                    onClick = { triggerRealDistanceCalculation() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_calculate_real_distance")
                ) {
                    if (isCalculatingDistance) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculando Ruta Real...", fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calcular Distancia Real por GPS / Coordenadas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Detalles del Paquete (Máximo 20 kg)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Paquete") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_package_type")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        packageTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Peso Estimado: ${String.format("%.1f", weightKg)} kg",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = weightKg.toFloat(),
                    onValueChange = { weightKg = it.toDouble() },
                    valueRange = 0.5f..20f,
                    steps = 38,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_weight")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas de Recolección / Entrega") },
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_order_notes")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Distance & Price Calculation Card (Automatic $9 MXN / km)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Cotización Automática",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distancia Estimada: ${String.format("%.1f", distanceKm)} km",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        color = YaVaYellowPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tarifa Base \$9 MXN / km",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Open Route in Google Maps button
                Button(
                    onClick = {
                        GpsLocationHelper.openGoogleMapsRoute(
                            context = context,
                            originLat = originLat,
                            originLng = originLng,
                            destLat = destLat,
                            destLng = destLng
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_google_maps_route")
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🗺️ Ver Ruta Real en Google Maps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total a Pagar:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\$" + String.format(java.util.Locale.US, "%.2f", quote.finalPriceMxn) + " MXN",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = YaVaYellowPrimary
                        )
                        Text(
                            text = "Comisión Plataforma (15%): \$${quote.platformCommissionAmountMxn} MXN",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tiempo Estimado:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~${quote.estimatedTimeMinutes} mins",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pago Socio: \$${quote.driverEarningsMxn} MXN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YaVaGreenSuccess
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Method & Payer Selection Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Condiciones de Cobro y Método de Pago",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de Quién Paga
                Text(
                    text = "1. ¿Quién realiza el pago del servicio?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    payerOptions.forEach { option ->
                        val isSelected = selectedPayer == option
                        OutlinedButton(
                            onClick = { selectedPayer = option },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) YaVaYellowPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) YaVaYellowPrimary else Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("payer_option_${option.replace(" ", "_")}")
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Método de Pago
                Text(
                    text = "2. Selecciona el Método de Pago:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentMethodOptions.forEach { (code, label) ->
                        val isSelected = selectedPaymentMethod == code
                        Surface(
                            onClick = { selectedPaymentMethod = code },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_method_$code")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = code }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when (code) {
                                            "EFECTIVO" -> "Pago directo al socio repartidor al momento del servicio"
                                            "TERMINAL" -> "Cobro con tarjeta de débito/crédito mediante terminal del repartidor"
                                            else -> "Transferencia electrónica directa a la cuenta oficial YaVa!"
                                        },
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Show SPEI Bank Details if Transferencia is selected
                if (selectedPaymentMethod == "TRANSFERENCIA") {
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🏦 Datos Oficiales para Transferencia SPEI:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "CLABE Interbancaria: 722969010374423450",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                            Text(text = "Banco / Plataforma: MercadoPago", fontSize = 12.sp, color = Color.DarkGray)
                            Text(text = "Titular / Nombre: YaVa", fontSize = 12.sp, color = Color.DarkGray)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ℹ️ Una vez realizado el pago, el Director lo verificará en el panel de control.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(
                                            androidx.compose.ui.text.AnnotatedString("722969010374423450")
                                        )
                                        Toast.makeText(context, "CLABE 722969010374423450 copiada", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("copy_clabe_button")
                                ) {
                                    Text("Copiar CLABE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Direct Confirmation Button (No redundant acceptance checkboxes)
        Button(
            onClick = {
                viewModel.submitOrderRequest(
                    clientName = clientName,
                    clientPhone = clientPhone,
                    clientEmail = clientEmail,
                    originAddress = originAddress,
                    destinationAddress = destinationAddress,
                    packageType = selectedType,
                    weightKg = weightKg,
                    distanceKm = distanceKm,
                    notes = notes,
                    payer = selectedPayer,
                    paymentMethod = selectedPaymentMethod,
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destLat,
                    destLng = destLng
                )
                onOrderCreatedAndTrack()
            },
            enabled = clientName.isNotBlank() && clientPhone.isNotBlank() && originAddress.isNotBlank() && destinationAddress.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = YaVaYellowPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_shipment_request_button")
        ) {
            Text(
                text = "Confirmar y Solicitar Envío",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(16.dp))

        YaVaContactCard(
            modifier = Modifier.fillMaxWidth(),
            customMessage = "Hola YaVa!, quiero solicitar un envío especial desde $originAddress hacia $destinationAddress."
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}
