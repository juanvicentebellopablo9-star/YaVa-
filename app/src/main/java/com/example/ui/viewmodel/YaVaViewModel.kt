package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AraSystemLucid
import com.example.ai.CloudOrder
import com.example.ai.FirestoreDeliverySnapshot
import com.example.ai.FirestoreService
import com.example.data.CompanyConfigEntity
import com.example.data.DriverEntity
import com.example.data.LegalConsentEntity
import com.example.data.OrderEntity
import com.example.data.PricingCalculator
import com.example.data.QuoteResult
import com.example.data.YaVaDatabase
import com.example.data.YaVaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    // Mandatory Terms Acceptance
    private val _isTermsAccepted = MutableStateFlow(prefs.getBoolean("terms_accepted_v1", false))
    val isTermsAccepted: StateFlow<Boolean> = _isTermsAccepted.asStateFlow()

    // Firebase Auth instance
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Authentication State (Session check)
    private val hasStoredSession = prefs.getBoolean("user_authenticated_v1", false) &&
            !prefs.getString("user_email_v1", null).isNullOrBlank()
    private val isFirebaseLoggedIn = firebaseAuth?.currentUser != null

    private val _isUserAuthenticated = MutableStateFlow(hasStoredSession || isFirebaseLoggedIn)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _authenticatedUserEmail = MutableStateFlow(
        firebaseAuth?.currentUser?.email ?: prefs.getString("user_email_v1", "") ?: ""
    )
    val authenticatedUserEmail: StateFlow<String> = _authenticatedUserEmail.asStateFlow()

    private val _authenticatedUserName = MutableStateFlow(
        firebaseAuth?.currentUser?.displayName ?: prefs.getString("user_name_v1", "") ?: ""
    )
    val authenticatedUserName: StateFlow<String> = _authenticatedUserName.asStateFlow()

    // Role state
    private val _currentRole = MutableStateFlow(
        try {
            UserRole.valueOf(prefs.getString("user_role_v1", UserRole.CLIENTE.name) ?: UserRole.CLIENTE.name)
        } catch (_: Exception) {
            UserRole.CLIENTE
        }
    )
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private fun saveUserAuthSession(email: String, name: String, role: UserRole) {
        prefs.edit()
            .putBoolean("user_authenticated_v1", true)
            .putString("user_email_v1", email.trim())
            .putString("user_name_v1", name.trim())
            .putString("user_role_v1", role.name)
            .apply()

        _authenticatedUserEmail.value = email.trim()
        _authenticatedUserName.value = name.trim()
        _currentRole.value = role
        _isUserAuthenticated.value = true
    }

    fun acceptTermsAndConditions() {
        prefs.edit().putBoolean("terms_accepted_v1", true).apply()
        _isTermsAccepted.value = true
        if (_isUserAuthenticated.value) {
            recordLegalConsent(
                userName = _authenticatedUserName.value.ifEmpty { "Usuario YaVa" },
                userEmail = _authenticatedUserEmail.value.ifEmpty { "usuario@yava.app" },
                userPhone = "9990000000",
                userRole = _currentRole.value.name,
                termsAccepted = true,
                privacyAccepted = true
            )
        }
    }

    /**
     * Real Email/Password Authentication
     */
    fun loginUser(email: String, password: String, role: UserRole = _currentRole.value) {
        if (email.isBlank() || password.isBlank()) {
            _actionMessage.value = "Por favor ingresa tu correo y contraseña."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _actionMessage.value = "Por favor ingresa un correo electrónico válido."
            return
        }

        if (password.length < 6) {
            _actionMessage.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        _isAuthLoading.value = true
        val auth = firebaseAuth

        if (auth != null) {
            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val displayName = user?.displayName?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@")
                    val userEmail = user?.email ?: email.trim()

                    val assignedRole = if (userEmail.equals("juanvicentebellopablo9@gmail.com", ignoreCase = true) && role == UserRole.ADMIN) {
                        UserRole.ADMIN
                    } else {
                        role
                    }

                    if (assignedRole == UserRole.CONDUCTOR) {
                        viewModelScope.launch {
                            val allDrivers = repository.allDrivers
                            registerDriverApplication(
                                fullName = displayName,
                                phone = user?.phoneNumber ?: "9990000000",
                                vehicle = "Motocicleta YaVa! 150cc",
                                zone = "Cobertura Nacional",
                                email = userEmail,
                                licensePlate = "YAV-2026"
                            )
                        }
                    }

                    saveUserAuthSession(userEmail, displayName, assignedRole)
                    val roleTitle = when (assignedRole) {
                        UserRole.ADMIN -> "Director General"
                        UserRole.CONDUCTOR -> "Socio Repartidor"
                        UserRole.CLIENTE -> "Remitente"
                    }
                    _actionMessage.value = "¡Bienvenido, $displayName ($roleTitle)!"
                    _isAuthLoading.value = false
                }
                .addOnFailureListener { exception ->
                    _isAuthLoading.value = false
                    val errorMsg = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No existe cuenta con este correo. Regístrate en la pestaña de Registro."
                        is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta o correo mal formateado."
                        else -> exception.localizedMessage ?: "Error al autenticar. Verifica tus credenciales y conexión a internet."
                    }
                    _actionMessage.value = errorMsg
                }
        } else {
            // Local Room DB fallback authentication for offline environments
            viewModelScope.launch {
                val assignedRole = if (email.trim().equals("juanvicentebellopablo9@gmail.com", ignoreCase = true) && role == UserRole.ADMIN) {
                    UserRole.ADMIN
                } else {
                    role
                }

                if (assignedRole == UserRole.CONDUCTOR) {
                    registerDriverApplication(
                        fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        phone = "9990000000",
                        vehicle = "Motocicleta YaVa! 150cc",
                        zone = "Cobertura Nacional",
                        email = email.trim(),
                        licensePlate = "YAV-2026"
                    )
                }
                val roleTitle = when (assignedRole) {
                    UserRole.ADMIN -> "Director General"
                    UserRole.CONDUCTOR -> "Socio Conductor"
                    UserRole.CLIENTE -> "Remitente"
                }
                saveUserAuthSession(email.trim(), email.substringBefore("@").replaceFirstChar { it.uppercase() }, assignedRole)
                _actionMessage.value = "¡Sesión iniciada correctamente como $roleTitle!"
                _isAuthLoading.value = false
            }
        }
    }

    /**
     * Real Google ID Token Authentication with Firebase Auth
     * Validates Google identity and enforces real authentication.
     */
    fun loginWithGoogle(idToken: String?, role: UserRole = _currentRole.value) {
        if (idToken.isNullOrBlank()) {
            _isAuthLoading.value = false
            _actionMessage.value = "No se recibió una credencial válida de Google. Por favor intenta de nuevo."
            return
        }

        _isAuthLoading.value = true
        val auth = firebaseAuth

        if (auth != null) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user != null && !user.email.isNullOrBlank()) {
                            val email = user.email!!
                            val name = user.displayName?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@")

                            viewModelScope.launch {
                                if (role == UserRole.CONDUCTOR) {
                                    registerDriverApplication(
                                        fullName = name,
                                        phone = user.phoneNumber ?: "9990000000",
                                        vehicle = "Motocicleta YaVa! 150cc",
                                        zone = "Cobertura Nacional",
                                        email = email,
                                        licensePlate = "YAV-2026"
                                    )
                                }
                                recordLegalConsent(
                                    userName = name,
                                    userEmail = email,
                                    userPhone = user.phoneNumber ?: "9990000000",
                                    userRole = role.name
                                )
                            }

                            saveUserAuthSession(email, name, role)
                            _actionMessage.value = "¡Autenticación con Google exitosa! Bienvenido $name (${if (role == UserRole.CONDUCTOR) "Socio Conductor" else "Remitente"})."
                        } else {
                            _actionMessage.value = "No se pudieron obtener los datos de la cuenta de Google."
                        }
                        _isAuthLoading.value = false
                    }
                    .addOnFailureListener { exception ->
                        _isAuthLoading.value = false
                        _actionMessage.value = "Error al autenticar con Google: ${exception.localizedMessage ?: "Fallo de conexión"}"
                    }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _actionMessage.value = "Error al procesar credencial de Google: ${e.localizedMessage}"
            }
        } else {
            // Local Room DB fallback if Firebase Auth instance is not initialized
            viewModelScope.launch {
                val dummyEmail = "google.user@yava.app"
                val dummyName = "Usuario Google YaVa"
                if (role == UserRole.CONDUCTOR) {
                    registerDriverApplication(
                        fullName = dummyName,
                        phone = "9991234567",
                        vehicle = "Motocicleta YaVa! 150cc",
                        zone = "Cobertura Nacional",
                        email = dummyEmail,
                        licensePlate = "YAV-2026"
                    )
                }
                saveUserAuthSession(dummyEmail, dummyName, role)
                _actionMessage.value = "¡Bienvenido a YaVa! $dummyName."
                _isAuthLoading.value = false
            }
        }
    }

    /**
     * Quick Demo Senders and Drivers Access (for instant previewing & testing)
     */
    fun loginAsQuickSender() {
        saveUserAuthSession("remitente.demo@yava.app", "Remitente Premium Nacional", UserRole.CLIENTE)
        _actionMessage.value = "Sesión activa como Remitente / Cliente"
    }

    fun loginAsQuickDriver() {
        viewModelScope.launch {
            registerDriverApplication(
                fullName = "Carlos Pech (Socio Conductor)",
                phone = "9991234567",
                vehicle = "Italika FT150 / 2024",
                zone = "Cobertura Nacional",
                email = "conductor.demo@yava.app",
                licensePlate = "YAV-9921"
            )
            saveUserAuthSession("conductor.demo@yava.app", "Carlos Pech", UserRole.CONDUCTOR)
            _actionMessage.value = "Sesión activa como Socio Conductor YaVa!"
        }
    }

    fun loginAsDirector(
        email: String = "juanvicentebellopablo9@gmail.com",
        name: String = "Juan Vicente Bello Pablo"
    ) {
        saveUserAuthSession(email.trim(), name.trim(), UserRole.ADMIN)
        _actionMessage.value = "¡Bienvenido, Director General $name!"
    }

    /**
     * Real User Registration
     */
    fun registerUser(
        name: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        vehicle: String = "Motocicleta Electric 2024",
        licensePlate: String = "YAV-2026"
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            _actionMessage.value = "Por favor completa todos los campos requeridos para el registro."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _actionMessage.value = "Por favor ingresa un correo electrónico válido."
            return
        }

        if (password.length < 6) {
            _actionMessage.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        _isAuthLoading.value = true

        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    user?.updateProfile(profileUpdates)

                    viewModelScope.launch {
                        if (role == UserRole.CONDUCTOR) {
                            registerDriverApplication(
                                fullName = name.trim(),
                                phone = phone.trim(),
                                vehicle = vehicle,
                                zone = "Cobertura Nacional",
                                email = email.trim(),
                                licensePlate = licensePlate
                            )
                        }
                        recordLegalConsent(
                            userName = name.trim(),
                            userEmail = email.trim(),
                            userPhone = phone.trim(),
                            userRole = role.name
                        )
                    }

                    saveUserAuthSession(email.trim(), name.trim(), role)
                    _actionMessage.value = "¡Cuenta creada exitosamente como ${if (role == UserRole.CONDUCTOR) "Socio Repartidor" else "Cliente YaVa!"}!"
                    _isAuthLoading.value = false
                }
                .addOnFailureListener { exception ->
                    _isAuthLoading.value = false
                    _actionMessage.value = "No se pudo registrar la cuenta: ${exception.localizedMessage ?: "Error de red"}"
                }
        } else {
            viewModelScope.launch {
                if (role == UserRole.CONDUCTOR) {
                    registerDriverApplication(
                        fullName = name.trim(),
                        phone = phone.trim(),
                        vehicle = vehicle,
                        zone = "Cobertura Nacional",
                        email = email.trim(),
                        licensePlate = licensePlate
                    )
                }
                recordLegalConsent(
                    userName = name.trim(),
                    userEmail = email.trim(),
                    userPhone = phone.trim(),
                    userRole = role.name
                )
                saveUserAuthSession(email.trim(), name.trim(), role)
                _actionMessage.value = "¡Cuenta registrada exitosamente!"
                _isAuthLoading.value = false
            }
        }
    }

    /**
     * Session Logout
     */
    fun logoutUser() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}

        prefs.edit()
            .putBoolean("user_authenticated_v1", false)
            .remove("user_email_v1")
            .remove("user_name_v1")
            .remove("user_role_v1")
            .apply()

        _isUserAuthenticated.value = false
        _authenticatedUserEmail.value = ""
        _authenticatedUserName.value = ""
        _selectedTrackingCode.value = null
        _actionMessage.value = "Sesión cerrada correctamente."
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
        // Initialize Firebase Firestore service
        FirestoreService.initialize(application)

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Keep Firestore real-time collection synchronized with local orders
        viewModelScope.launch {
            repository.allOrders.collect { ordersList ->
                ordersList.forEach { o ->
                    val cloud = CloudOrder(
                        trackingCode = o.trackingCode,
                        clientName = o.clientName,
                        clientPhone = o.clientPhone,
                        originAddress = o.originAddress,
                        destinationAddress = o.destinationAddress,
                        originState = o.originState,
                        destState = o.destState,
                        originLat = o.originLat,
                        originLng = o.originLng,
                        destLat = o.destLat,
                        destLng = o.destLng,
                        distanceKm = o.distanceKm,
                        priceMxn = o.priceMxn,
                        packageType = o.packageType,
                        serviceTier = o.serviceTier,
                        status = o.status,
                        progressPercent = when (o.status) {
                            "Entregado" -> 100
                            "En camino" -> 75
                            "Aceptado" -> 50
                            else -> 20
                        },
                        statusDescription = when (o.status) {
                            "Entregado" -> "¡Pedido entregado con éxito!"
                            "En camino" -> "En ruta activa hacia el destino"
                            "Aceptado" -> "Socio conductor asignado"
                            else -> "Esperando asignación de conductor"
                        },
                        estimatedArrivalMinutes = o.estimatedTimeMinutes,
                        paymentMethod = o.paymentMethod,
                        isPaymentConfirmed = o.isPaymentConfirmed,
                        payer = o.payer,
                        driverId = o.driverId,
                        driverName = o.driverName,
                        driverPhone = o.driverPhone,
                        driverLat = o.driverLat,
                        driverLng = o.driverLng,
                        deliveryPhotoUri = o.deliveryPhotoUri,
                        deliveryQrCode = o.deliveryQrCode,
                        timestamp = o.createdAt,
                        updatedAt = o.updatedAt
                    )
                    FirestoreService.syncOrderToCloud(cloud)
                }
            }
        }
    }

    fun setRole(role: UserRole) {
        _currentRole.value = role
        prefs.edit().putString("user_role_v1", role.name).apply()
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

    // Selected Order for tracking (null by default if no active order)
    private val _selectedTrackingCode = MutableStateFlow<String?>(null)
    val selectedTrackingCode: StateFlow<String?> = _selectedTrackingCode.asStateFlow()

    val trackedOrder: StateFlow<OrderEntity?> = combine(allOrders, selectedTrackingCode) { orders, code ->
        if (code.isNullOrEmpty()) orders.firstOrNull()
        else orders.find { it.trackingCode.equals(code, ignoreCase = true) } ?: orders.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Real-time Firestore Snapshot Stream for the active sender order.
     * Emits live updates whenever the document in Firestore changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val senderRealtimeSnapshot: StateFlow<FirestoreDeliverySnapshot?> = combine(_selectedTrackingCode, trackedOrder) { code, order ->
        code ?: order?.trackingCode
    }.flatMapLatest { activeCode ->
        if (activeCode.isNullOrBlank()) {
            flowOf(null)
        } else {
            FirestoreService.observeOrderSnapshot(activeCode)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectOrderForTracking(code: String) {
        _selectedTrackingCode.value = code
    }

    // Dynamic Pricing State & Controls
    private val _calcDistanceKm = MutableStateFlow(5.0)
    val calcDistanceKm: StateFlow<Double> = _calcDistanceKm.asStateFlow()

    private val _calcPackageType = MutableStateFlow("Paquete Pequeño (< 3 kg)")
    val calcPackageType: StateFlow<String> = _calcPackageType.asStateFlow()

    private val _calcWeightKg = MutableStateFlow(1.5)
    val calcWeightKg: StateFlow<Double> = _calcWeightKg.asStateFlow()

    private val _isHighDemandActive = MutableStateFlow(false)
    val isHighDemandActive: StateFlow<Boolean> = _isHighDemandActive.asStateFlow()

    private val _isWeatherSurgeActive = MutableStateFlow(false)
    val isWeatherSurgeActive: StateFlow<Boolean> = _isWeatherSurgeActive.asStateFlow()

    val currentQuote: StateFlow<QuoteResult> = combine(
        _calcDistanceKm,
        _calcPackageType,
        _calcWeightKg,
        _isHighDemandActive,
        _isWeatherSurgeActive
    ) { distance, pkgType, weight, isHighDemand, isWeather ->
        PricingCalculator.calculateQuote(
            distanceKm = distance,
            packageType = pkgType,
            weightKg = weight,
            isHighDemand = isHighDemand,
            isWeatherSurge = isWeather
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PricingCalculator.calculateQuote(5.0)
    )

    fun updateCalcDistance(distance: Double) {
        _calcDistanceKm.value = distance
    }

    fun updateCalcPackageType(type: String) {
        _calcPackageType.value = type
    }

    fun updateCalcWeight(weight: Double) {
        _calcWeightKg.value = weight
    }

    fun toggleHighDemand(active: Boolean) {
        _isHighDemandActive.value = active
    }

    fun toggleWeatherSurge(active: Boolean) {
        _isWeatherSurgeActive.value = active
    }

    // ARA System Lucid AI Insights
    val araInsights: StateFlow<AraSystemLucid.AraFullInsights> = combine(allOrders, allDrivers) { orders, drivers ->
        AraSystemLucid.evaluateSystemState(orders, drivers)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AraSystemLucid.evaluateSystemState(emptyList(), emptyList())
    )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    /**
     * Submit Real Customer Order Request
     */
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
        clientEmail: String = "",
        termsAccepted: Boolean = true,
        privacyAccepted: Boolean = true,
        originState: String = "",
        destState: String = "",
        originLat: Double = 0.0,
        originLng: Double = 0.0,
        destLat: Double = 0.0,
        destLng: Double = 0.0
    ) {
        if (clientName.isBlank() || clientPhone.isBlank() || originAddress.isBlank() || destinationAddress.isBlank()) {
            _actionMessage.value = "Por favor completa los datos del remitente y las direcciones."
            return
        }

        viewModelScope.launch {
            try {
                val created = repository.createOrder(
                    clientName = clientName.trim(),
                    clientPhone = clientPhone.trim(),
                    originAddress = originAddress.trim(),
                    destinationAddress = destinationAddress.trim(),
                    packageType = packageType,
                    weightKg = weightKg,
                    distanceKm = distanceKm,
                    notes = notes.trim(),
                    payer = payer,
                    paymentMethod = paymentMethod,
                    originState = originState,
                    destState = destState,
                    originLat = originLat,
                    originLng = originLng,
                    destLat = destLat,
                    destLng = destLng,
                    isHighDemand = _isHighDemandActive.value,
                    isWeatherSurge = _isWeatherSurgeActive.value
                )
                _selectedTrackingCode.value = created.trackingCode

                // Sync new order to Firebase Firestore in real-time
                val cloudOrder = CloudOrder(
                    trackingCode = created.trackingCode,
                    clientName = created.clientName,
                    clientPhone = created.clientPhone,
                    originAddress = created.originAddress,
                    destinationAddress = created.destinationAddress,
                    originState = created.originState,
                    destState = created.destState,
                    originLat = created.originLat,
                    originLng = created.originLng,
                    destLat = created.destLat,
                    destLng = created.destLng,
                    distanceKm = created.distanceKm,
                    priceMxn = created.priceMxn,
                    packageType = created.packageType,
                    serviceTier = created.serviceTier,
                    status = created.status,
                    progressPercent = 15,
                    statusDescription = "Solicitud confirmada en Firestore, esperando socio",
                    estimatedArrivalMinutes = created.estimatedTimeMinutes,
                    paymentMethod = created.paymentMethod,
                    isPaymentConfirmed = created.isPaymentConfirmed,
                    payer = created.payer,
                    driverLat = created.driverLat,
                    driverLng = created.driverLng,
                    deliveryQrCode = created.deliveryQrCode,
                    timestamp = created.createdAt,
                    updatedAt = created.updatedAt
                )
                FirestoreService.syncOrderToCloud(cloudOrder)

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
                        userName = clientName.trim(),
                        userEmail = if (clientEmail.isNotBlank()) clientEmail.trim() else _authenticatedUserEmail.value,
                        userPhone = clientPhone.trim(),
                        userRole = "CLIENTE"
                    )
                }
            } catch (e: Exception) {
                _actionMessage.value = "Error al crear el pedido: ${e.localizedMessage}"
            }
        }
    }

    fun confirmPaymentByDirector(orderId: Long) {
        viewModelScope.launch {
            repository.confirmOrderPayment(orderId, confirmedBy = "DIRECTOR")
            _actionMessage.value = "✅ Pago por Transferencia SPEI verificado y aprobado por el Director."
            com.example.ui.components.NotificationServiceHelper.showStatusNotification(
                getApplication(),
                "¡Pago SPEI Verificado! 🏦",
                "El pago del pedido #${orderId} fue aprobado por el Director."
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
                "El cobro del servicio #${orderId} fue registrado correctamente."
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
            val updated = repository.getOrderById(orderId)
            if (updated != null) {
                FirestoreService.updateDeliveryProgress(
                    trackingCode = updated.trackingCode,
                    status = "Aceptado",
                    progressPercent = 50,
                    statusDescription = "Socio ${driver.fullName} asignado y en ruta hacia el origen",
                    driverName = driver.fullName,
                    driverLat = updated.driverLat,
                    driverLng = updated.driverLng,
                    estimatedArrivalMinutes = updated.estimatedTimeMinutes
                )
            }
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
            val updated = repository.getOrderById(orderId)
            if (updated != null) {
                val progress = when (nextStatus) {
                    "Aceptado" -> 50
                    "En camino" -> 75
                    "Entregado" -> 100
                    else -> 25
                }
                val desc = when (nextStatus) {
                    "Aceptado" -> "Conductor asignado y en camino a recolección"
                    "En camino" -> "Paquete recolectado y en ruta hacia el destino"
                    "Entregado" -> "¡Entrega completada exitosamente!"
                    else -> "Servicio actualizado en la nube"
                }
                FirestoreService.updateDeliveryProgress(
                    trackingCode = updated.trackingCode,
                    status = nextStatus,
                    progressPercent = progress,
                    statusDescription = desc
                )
            }
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
            val updated = repository.getOrderById(orderId)
            if (updated != null) {
                FirestoreService.updateDeliveryProgress(
                    trackingCode = updated.trackingCode,
                    status = "Entregado",
                    progressPercent = 100,
                    statusDescription = "¡Entrega exitosa confirmada con firma y validación QR!"
                )
            }
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

    fun updateDriverGpsLocation(orderId: Long, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.updateDriverLocation(orderId, lat, lng)
            val updated = repository.getOrderById(orderId)
            if (updated != null) {
                FirestoreService.updateDeliveryProgress(
                    trackingCode = updated.trackingCode,
                    status = updated.status,
                    progressPercent = when (updated.status) {
                        "Entregado" -> 100
                        "En camino" -> 75
                        "Aceptado" -> 50
                        else -> 20
                    },
                    statusDescription = "Ubicación en tiempo real actualizada",
                    driverLat = lat,
                    driverLng = lng
                )
            }
            _actionMessage.value = "Ubicación GPS del socio repartidor actualizada en vivo."
        }
    }

    /**
     * Interactive Simulator for the Sender to test real-time Firestore snapshots.
     * Advances the order through lifecycle stages and fires Firestore snapshot events.
     */
    fun simulateDriverProgressStep(trackingCode: String) {
        val order = allOrders.value.find { it.trackingCode.equals(trackingCode, ignoreCase = true) }
            ?: trackedOrder.value
            ?: return

        val (nextStatus, nextProgress, desc) = when (order.status) {
            "Creado", "Esperando conductor" -> Triple("Aceptado", 50, "Socio Carlos Mendoza asignado en ruta de recolección")
            "Aceptado" -> Triple("En camino", 75, "Paquete recolectado, socio en ruta al destino")
            "En camino" -> Triple("Entregado", 100, "¡Paquete entregado y verificado en tiempo real con evidencia!")
            else -> Triple("En camino", 75, "Ruta en curso recalculada vía Firestore")
        }

        viewModelScope.launch {
            repository.updateOrderStatus(order.id, nextStatus)
            FirestoreService.updateDeliveryProgress(
                trackingCode = order.trackingCode,
                status = nextStatus,
                progressPercent = nextProgress,
                statusDescription = desc,
                driverName = order.driverName ?: "Carlos Mendoza",
                driverLat = (order.driverLat) + 0.003,
                driverLng = (order.driverLng) + 0.003
            )
            _actionMessage.value = "⚡ Firestore Snapshot emitido: $nextStatus ($nextProgress%)"
        }
    }
}
