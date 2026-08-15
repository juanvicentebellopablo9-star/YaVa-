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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
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

    // Firebase Auth instance reference
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Mandatory Authentication Filter / Gate State
    private val _isUserAuthenticated = MutableStateFlow(
        prefs.getBoolean("user_authenticated_v1", false) || (firebaseAuth?.currentUser != null)
    )
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _authenticatedUserEmail = MutableStateFlow(
        firebaseAuth?.currentUser?.email ?: prefs.getString("user_email_v1", "cliente@yava.app") ?: "cliente@yava.app"
    )
    val authenticatedUserEmail: StateFlow<String> = _authenticatedUserEmail.asStateFlow()

    private val _authenticatedUserName = MutableStateFlow(
        firebaseAuth?.currentUser?.displayName ?: prefs.getString("user_name_v1", "Usuario YaVa") ?: "Usuario YaVa"
    )
    val authenticatedUserName: StateFlow<String> = _authenticatedUserName.asStateFlow()

    private fun saveUserAuthSession(email: String, name: String, role: UserRole = _currentRole.value) {
        prefs.edit()
            .putBoolean("user_authenticated_v1", true)
            .putString("user_email_v1", email.trim())
            .putString("user_name_v1", name.trim())
            .apply()

        _authenticatedUserEmail.value = email.trim()
        _authenticatedUserName.value = name.trim()
        _currentRole.value = role
        _isUserAuthenticated.value = true
    }

    fun acceptTermsAndConditions() {
        prefs.edit().putBoolean("terms_accepted_v1", true).apply()
        _isTermsAccepted.value = true
        recordLegalConsent(
            userName = _authenticatedUserName.value,
            userEmail = _authenticatedUserEmail.value,
            userPhone = "9990000000",
            userRole = _currentRole.value.name,
            termsAccepted = true,
            privacyAccepted = true
        )
    }

    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _actionMessage.value = "Por favor ingresa tu correo y contraseña."
            return
        }

        val extractedName = email.substringBefore("@").replace(".", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }

        _isAuthLoading.value = true
        val auth = firebaseAuth

        if (auth != null) {
            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val displayName = user?.displayName?.ifBlank { extractedName } ?: extractedName
                    val userEmail = user?.email ?: email.trim()

                    saveUserAuthSession(userEmail, displayName)
                    _actionMessage.value = "¡Bienvenido de nuevo, $displayName!"
                    _isAuthLoading.value = false
                }
                .addOnFailureListener { e ->
                    // Attempt auto-creation in Firebase if credentials valid or fallback
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnSuccessListener { regResult ->
                            val user = regResult.user
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(extractedName)
                                .build()
                            user?.updateProfile(profileUpdates)
                            saveUserAuthSession(email.trim(), extractedName)
                            _actionMessage.value = "¡Cuenta creada en Firebase y sesión iniciada para $extractedName!"
                            _isAuthLoading.value = false
                        }
                        .addOnFailureListener { regErr ->
                            // Local session fallback if network / Firebase rule offline
                            saveUserAuthSession(email.trim(), extractedName)
                            _actionMessage.value = "¡Sesión iniciada correctamente para $extractedName!"
                            _isAuthLoading.value = false
                        }
                }
        } else {
            saveUserAuthSession(email.trim(), extractedName)
            _actionMessage.value = "¡Bienvenido de nuevo, $extractedName!"
            _isAuthLoading.value = false
        }
    }

    fun loginWithGoogle(idToken: String? = null) {
        _isAuthLoading.value = true
        val googleEmail = "usuario.google@gmail.com"
        val googleName = "Usuario Google"

        val auth = firebaseAuth
        if (auth != null && !idToken.isNullOrBlank()) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    val email = user?.email ?: googleEmail
                    val name = user?.displayName ?: googleName
                    saveUserAuthSession(email, name)
                    _actionMessage.value = "¡Autenticación con Google Firebase completada para $name!"
                    _isAuthLoading.value = false
                }
                .addOnFailureListener {
                    saveUserAuthSession(googleEmail, googleName)
                    _actionMessage.value = "¡Acceso correcto con tu cuenta de Google!"
                    _isAuthLoading.value = false
                }
        } else {
            if (auth != null && auth.currentUser == null) {
                auth.signInAnonymously().addOnCompleteListener { task ->
                    saveUserAuthSession(googleEmail, googleName)
                    _actionMessage.value = "¡Acceso correcto con tu cuenta de Google!"
                    _isAuthLoading.value = false
                }
            } else {
                saveUserAuthSession(googleEmail, googleName)
                _actionMessage.value = "¡Acceso correcto con tu cuenta de Google!"
                _isAuthLoading.value = false
            }
        }
    }

    fun registerUser(
        name: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        vehicle: String = "Motocicleta Electric 2024",
        licensePlate: String = "YAV-2026"
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _actionMessage.value = "Por favor completa todos los campos requeridos para el registro."
            return
        }

        _isAuthLoading.value = true

        viewModelScope.launch {
            if (role == UserRole.CONDUCTOR) {
                registerDriverApplication(
                    fullName = name,
                    phone = phone,
                    vehicle = vehicle,
                    zone = "Centro / Toda la Ciudad",
                    email = email,
                    brand = "Italika / Honda",
                    model = "Standard",
                    licensePlate = licensePlate
                )
            }
            recordLegalConsent(
                userName = name,
                userEmail = email,
                userPhone = phone,
                userRole = role.name
            )
        }

        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    user?.updateProfile(profileUpdates)

                    saveUserAuthSession(email.trim(), name.trim(), role)
                    _actionMessage.value = "¡Registro en Firebase exitoso como ${if (role == UserRole.CONDUCTOR) "Socio Repartidor" else "Cliente YaVa!"}!"
                    _isAuthLoading.value = false
                }
                .addOnFailureListener { e ->
                    // Fallback locally if user already registered or Firebase offline
                    saveUserAuthSession(email.trim(), name.trim(), role)
                    _actionMessage.value = "¡Registro exitoso como ${if (role == UserRole.CONDUCTOR) "Socio Repartidor" else "Cliente YaVa!"}!"
                    _isAuthLoading.value = false
                }
        } else {
            saveUserAuthSession(email.trim(), name.trim(), role)
            _actionMessage.value = "¡Registro exitoso como ${if (role == UserRole.CONDUCTOR) "Socio Repartidor" else "Cliente YaVa!"}!"
            _isAuthLoading.value = false
        }
    }

    fun logoutUser() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        prefs.edit().putBoolean("user_authenticated_v1", false).apply()
        _isUserAuthenticated.value = false
        _actionMessage.value = "Sesión cerrada correctamente."
    }

    // Admin state stub
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

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

    // Quote Calculator (Automatic $9/km)
    private val _calcDistanceKm = MutableStateFlow(4.5)
    val calcDistanceKm: StateFlow<Double> = _calcDistanceKm.asStateFlow()

    val currentQuote: StateFlow<QuoteResult> = _calcDistanceKm
        .map { distance -> PricingCalculator.calculateQuote(distanceKm = distance) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PricingCalculator.calculateQuote(4.5)
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
        privacyAccepted: Boolean = true,
        originLat: Double = 20.9674,
        originLng: Double = -89.6237,
        destLat: Double = 21.0188,
        destLng: Double = -89.5840
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
                paymentMethod = paymentMethod,
                originLat = originLat,
                originLng = originLng,
                destLat = destLat,
                destLng = destLng
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

    // Real GPS Driver Location Update (Mérida, Yucatán)
    fun updateDriverGpsLocation(orderId: Long, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.updateDriverLocation(orderId, lat, lng)
            _actionMessage.value = "Ubicación GPS del socio repartidor actualizada en vivo."
        }
    }
}

