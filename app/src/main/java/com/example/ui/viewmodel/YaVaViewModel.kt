package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AraSystemLucid
import com.example.data.CompanyConfigEntity
import com.example.data.DriverEntity
import com.example.data.LegalConsentEntity
import com.example.data.OrderEntity
import com.example.data.PricingCalculator
import com.example.data.QuoteResult
import com.example.data.YaVaDatabase
import com.example.data.YaVaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserRole {
    CLIENTE,
    CONDUCTOR,
    ADMIN
}

class YaVaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: YaVaRepository
    private val prefs = application.getSharedPreferences("yava_prefs", Context.MODE_PRIVATE)

    // Mandatory Terms Acceptance (First-time launch)
    private val _isTermsAccepted = MutableStateFlow(prefs.getBoolean("terms_accepted_v1", false))
    val isTermsAccepted: StateFlow<Boolean> = _isTermsAccepted.asStateFlow()

    fun acceptTermsAndConditions() {
        prefs.edit().putBoolean("terms_accepted_v1", true).apply()
        _isTermsAccepted.value = true
        recordLegalConsent(
            userName = "Usuario General YaVa",
            userEmail = "usuario@yava.app",
            userPhone = "9990000000",
            userRole = _currentRole.value.name,
            termsAccepted = true,
            privacyAccepted = true
        )
    }

    // Admin Director Authentication (Juan Vicente Bello Pablo)
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    fun authenticateAdmin(email: String, pass: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val isValid = (cleanEmail == "juanvicentebellopablo9@gmail.com" && pass == "Andrade23")
        if (isValid) {
            _isAdminAuthenticated.value = true
            _currentRole.value = UserRole.ADMIN
            _actionMessage.value = "¡Bienvenido Director Juan Vicente Bello Pablo!"
        } else {
            _isAdminAuthenticated.value = false
            _actionMessage.value = "Acceso Denegado. Credenciales de Director incorrectas."
        }
        return isValid
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
        if (_currentRole.value == UserRole.ADMIN) {
            _currentRole.value = UserRole.CLIENTE
        }
        _actionMessage.value = "Sesión de Director cerrada."
    }

    init {
        val db = YaVaDatabase.getDatabase(application)
        repository = YaVaRepository(
            db.orderDao(),
            db.driverDao(),
            db.userDao(),
            db.companyConfigDao(),
            db.legalConsentDao()
        )
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Role state
    private val _currentRole = MutableStateFlow(UserRole.CLIENTE)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    // Company Config & Legal Consents
    val companyConfig: StateFlow<CompanyConfigEntity> = repository.companyConfig
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanyConfigEntity())

    val legalConsents: StateFlow<List<LegalConsentEntity>> = repository.legalConsents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCompanyConfig(
        whatsappNumber: String,
        phoneNumber: String,
        supportEmail: String,
        facebookName: String,
        facebookUrl: String,
        operatingHours: String,
        emergencyNotice: String
    ) {
        viewModelScope.launch {
            val updatedConfig = CompanyConfigEntity(
                id = 1,
                whatsappNumber = whatsappNumber,
                phoneNumber = phoneNumber,
                supportEmail = supportEmail,
                facebookName = facebookName,
                facebookUrl = facebookUrl,
                operatingHours = operatingHours,
                emergencyNotice = emergencyNotice,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveCompanyConfig(updatedConfig)
            _actionMessage.value = "Configuración de empresa actualizada correctamente."
        }
    }

    fun recordLegalConsent(
        userName: String,
        userEmail: String,
        userPhone: String,
        userRole: String,
        termsAccepted: Boolean = true,
        privacyAccepted: Boolean = true
    ) {
        viewModelScope.launch {
            repository.saveLegalConsent(
                userName = userName,
                userEmail = userEmail,
                userPhone = userPhone,
                userRole = userRole,
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted,
                documentVersion = "v1.0-2026-MX"
            )
        }
    }

    // Live Orders & Drivers
    val allOrders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableOrders: StateFlow<List<OrderEntity>> = repository.availableOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDrivers: StateFlow<List<DriverEntity>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDrivers: StateFlow<List<DriverEntity>> = repository.pendingDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedDrivers: StateFlow<List<DriverEntity>> = repository.approvedAvailableDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Order for tracking
    private val _selectedTrackingCode = MutableStateFlow<String?>("YAVA-58219")
    val selectedTrackingCode: StateFlow<String?> = _selectedTrackingCode.asStateFlow()

    val trackedOrder: StateFlow<OrderEntity?> = combine(allOrders, selectedTrackingCode) { orders, code ->
        if (code.isNullOrEmpty()) orders.firstOrNull()
        else orders.find { it.trackingCode.equals(code, ignoreCase = true) } ?: orders.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectOrderForTracking(code: String) {
        _selectedTrackingCode.value = code
    }

    // Quote Calculator & Director Surge Settings
    private val _calcDistanceKm = MutableStateFlow(4.5)
    val calcDistanceKm: StateFlow<Double> = _calcDistanceKm.asStateFlow()

    private val _isDirectorWeatherSurgeActive = MutableStateFlow(false)
    val isDirectorWeatherSurgeActive: StateFlow<Boolean> = _isDirectorWeatherSurgeActive.asStateFlow()

    private val _isHighDemandActive = MutableStateFlow(false)
    val isHighDemandActive: StateFlow<Boolean> = _isHighDemandActive.asStateFlow()

    fun setDirectorWeatherSurge(active: Boolean) {
        _isDirectorWeatherSurgeActive.value = active
        _actionMessage.value = if (active) {
            "Ajuste por clima (Lluvia/Tormenta +15%) ACTIVADO manualmente por el Director."
        } else {
            "Ajuste por clima DESACTIVADO por el Director."
        }
    }

    fun setHighDemandActive(active: Boolean) {
        _isHighDemandActive.value = active
    }

    val currentQuote: StateFlow<QuoteResult> = combine(
        _calcDistanceKm,
        _isHighDemandActive,
        _isDirectorWeatherSurgeActive
    ) { distance, highDemand, weatherSurge ->
        PricingCalculator.calculateQuote(
            distanceKm = distance,
            isHighDemand = highDemand,
            isDirectorWeatherSurgeActive = weatherSurge
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PricingCalculator.calculateQuote(4.5, false, false)
    )

    fun updateCalcDistance(distance: Double) {
        _calcDistanceKm.value = distance
    }

    // ARA System Lucid AI Insights
    val araInsights: StateFlow<AraSystemLucid.AraFullInsights> = combine(allOrders, allDrivers) { orders, drivers ->
        AraSystemLucid.evaluateSystemState(orders, drivers)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AraSystemLucid.evaluateSystemState(emptyList(), emptyList())
    )

    // User / Driver Registration Actions
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun submitOrderRequest(
        clientName: String,
        clientPhone: String,
        originAddress: String,
        destinationAddress: String,
        packageType: String,
        weightKg: Double,
        distanceKm: Double,
        notes: String,
        payer: String = "Paga quien envía",
        paymentMethod: String = "EFECTIVO",
        clientEmail: String = "cliente@yava.app",
        termsAccepted: Boolean = true,
        privacyAccepted: Boolean = true
    ) {
        viewModelScope.launch {
            val created = repository.createOrder(
                clientName = clientName,
                clientPhone = clientPhone,
                originAddress = originAddress,
                destinationAddress = destinationAddress,
                packageType = packageType,
                weightKg = weightKg,
                distanceKm = distanceKm,
                notes = notes,
                payer = payer,
                paymentMethod = paymentMethod
            )
            _selectedTrackingCode.value = created.trackingCode
            
            val paymentInfoMsg = when (paymentMethod) {
                "TRANSFERENCIA" -> "Pago SPEI registrado (Pendiente de verificación en cuenta MercadoPago por Director)."
                "TERMINAL" -> "Cobro con Tarjeta por Terminal seleccionado."
                else -> "Pago en Efectivo al Conductor seleccionado."
            }

            _actionMessage.value = "¡Envío ${created.trackingCode} solicitado con éxito! Total: \$${created.priceMxn} MXN. $paymentInfoMsg"
            
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "¡Envío Solicitado con Éxito! 📦",
                "Tu código de rastreo es ${created.trackingCode}. $paymentInfoMsg"
            )

            if (termsAccepted && privacyAccepted) {
                recordLegalConsent(
                    userName = clientName,
                    userEmail = clientEmail,
                    userPhone = clientPhone,
                    userRole = "CLIENTE"
                )
            }
        }
    }

    fun confirmPaymentByDirector(orderId: Long) {
        viewModelScope.launch {
            repository.confirmOrderPayment(orderId, confirmedBy = "DIRECTOR")
            _actionMessage.value = "✅ Pago por Transferencia SPEI verificado y aceptado correctamente por el Director."
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "¡Pago SPEI Verificado! 🏦",
                "El pago por transferencia a MercadoPago (CLABE 722969010374423450) del pedido #${orderId} fue aprobado por el Director."
            )
        }
    }

    fun confirmPaymentByDriver(orderId: Long) {
        viewModelScope.launch {
            repository.confirmOrderPayment(orderId, confirmedBy = "CONDUCTOR")
            _actionMessage.value = "✅ Cobro / Pago Recibido confirmado por el Socio Repartidor."
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "¡Pago Confirmado por Repartidor! 🛵",
                "El cobro en efectivo/terminal del servicio #${orderId} fue registrado correctamente."
            )
        }
    }

    fun registerDriverApplication(
        fullName: String,
        phone: String,
        vehicle: String,
        zone: String,
        email: String = "",
        photoUri: String? = null,
        brand: String = "",
        model: String = "",
        year: Int = 2024,
        licensePlate: String = "",
        cargoCapacityKg: Double = 25.0,
        coverageZone: String = "",
        availableSchedule: String = "",
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        termsAccepted: Boolean = true,
        privacyAccepted: Boolean = true
    ) {
        viewModelScope.launch {
            val driver = repository.registerDriver(
                fullName = fullName,
                phone = phone,
                vehicle = vehicle,
                zone = zone,
                email = email,
                photoUri = photoUri,
                brand = brand,
                model = model,
                year = year,
                licensePlate = licensePlate,
                cargoCapacityKg = cargoCapacityKg,
                coverageZone = coverageZone,
                availableSchedule = availableSchedule,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                acceptedTermsVersion = "v1.0-2026-MX",
                locationPermissionGranted = true
            )
            recordLegalConsent(
                userName = fullName,
                userEmail = if (email.isNotEmpty()) email else "conductor@yava.app",
                userPhone = phone,
                userRole = "CONDUCTOR",
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted
            )
            _actionMessage.value = "Solicitud de socio enviada. Tu registro ID #${driver.id} está PENDIENTE DE APROBACIÓN por un Administrador."
        }
    }

    fun acceptOrderByDriver(orderId: Long, driver: DriverEntity) {
        viewModelScope.launch {
            repository.acceptOrder(orderId, driver)
            _actionMessage.value = "¡Pedido #${orderId} aceptado por ${driver.fullName}!"
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "Conductor Asignado 🛵",
                "${driver.fullName} ha aceptado tu pedido #${orderId} y va en camino a recolección."
            )
        }
    }

    fun advanceOrderStatus(orderId: Long, currentStatus: String) {
        val nextStatus = when (currentStatus) {
            "Creado" -> "Esperando conductor"
            "Esperando conductor" -> "Aceptado"
            "Aceptado" -> "En camino"
            "En camino" -> "Entregado"
            else -> currentStatus
        }
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, nextStatus)
            _actionMessage.value = "Estado del pedido actualizado a: $nextStatus"
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "Actualización de Envío #${orderId}",
                "El estado de tu paquete cambió a: $nextStatus."
            )
        }
    }

    fun completeDeliveryWithProof(orderId: Long, photoUri: String, qrCode: String) {
        viewModelScope.launch {
            repository.completeDelivery(orderId, photoUri, qrCode)
            _actionMessage.value = "¡Entrega confirmada con evidencia exitosamente!"
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "¡Pedido Entregado Con Éxito! 🎉",
                "El envío #${orderId} ha sido entregado. Puedes descargar tu recibo PDF en la app."
            )
        }
    }

    fun approveDriverByAdmin(driverId: Long) {
        viewModelScope.launch {
            repository.setDriverApproval(driverId, true)
            _actionMessage.value = "Socio Conductor ID #$driverId APROBADO con éxito."
        }
    }

    fun setDriverStatusByAdmin(driverId: Long, status: String) {
        viewModelScope.launch {
            repository.setDriverStatus(driverId, status)
            _actionMessage.value = "Estado del Socio Conductor ID #$driverId cambiado a: $status"
        }
    }

    // Driver location simulation movement along route
    fun simulateDriverMovement(orderId: Long) {
        viewModelScope.launch {
            val latSteps = listOf(19.4265, 19.4280, 19.4300, 19.4320, 19.4340)
            val lngSteps = listOf(-99.1678, -99.1620, -99.1550, -99.1480, -99.1400)

            for (i in latSteps.indices) {
                delay(2000)
                repository.updateDriverLocation(orderId, latSteps[i], lngSteps[i])
            }
            repository.updateOrderStatus(orderId, "Entregado")
            _actionMessage.value = "Simulación: El conductor ha llegado a su destino y entregó el paquete."
        }
    }
}

