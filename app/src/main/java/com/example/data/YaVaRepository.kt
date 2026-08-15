package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class YaVaRepository(
    private val orderDao: OrderDao,
    private val driverDao: DriverDao,
    private val userDao: UserDao,
    private val companyConfigDao: CompanyConfigDao,
    private val legalConsentDao: LegalConsentDao
) {
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val availableOrders: Flow<List<OrderEntity>> = orderDao.getAvailableOrders()
    val allDrivers: Flow<List<DriverEntity>> = driverDao.getAllDrivers()
    val pendingDrivers: Flow<List<DriverEntity>> = driverDao.getPendingDrivers()
    val approvedAvailableDrivers: Flow<List<DriverEntity>> = driverDao.getApprovedAvailableDrivers()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val companyConfig: Flow<CompanyConfigEntity?> = companyConfigDao.getCompanyConfig()
    val legalConsents: Flow<List<LegalConsentEntity>> = legalConsentDao.getAllLegalConsents()

    fun observeOrderByTracking(code: String): Flow<OrderEntity?> = orderDao.observeOrderByTrackingCode(code)

    fun getOrdersByDriver(driverId: Long): Flow<List<OrderEntity>> = orderDao.getOrdersByDriver(driverId)

    suspend fun saveCompanyConfig(config: CompanyConfigEntity) {
        companyConfigDao.saveCompanyConfig(config)
    }

    suspend fun saveLegalConsent(
        userName: String,
        userEmail: String,
        userPhone: String,
        userRole: String,
        termsAccepted: Boolean = true,
        privacyAccepted: Boolean = true,
        documentVersion: String = "v1.0-2026-MX"
    ): Long {
        val consent = LegalConsentEntity(
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            userRole = userRole,
            termsAccepted = termsAccepted,
            privacyAccepted = privacyAccepted,
            documentVersion = documentVersion,
            acceptedAt = System.currentTimeMillis()
        )
        return legalConsentDao.insertLegalConsent(consent)
    }

    suspend fun createOrder(
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
        originLat: Double = 19.4326,
        originLng: Double = -99.1332,
        destLat: Double = 19.4265,
        destLng: Double = -99.1678
    ): OrderEntity {
        val quote = PricingCalculator.calculateQuote(distanceKm)
        val trackingCode = "YAVA-${Random.nextInt(10000, 99999)}"

        val newOrder = OrderEntity(
            trackingCode = trackingCode,
            clientName = clientName,
            clientPhone = clientPhone,
            originAddress = originAddress,
            destinationAddress = destinationAddress,
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng,
            packageType = packageType,
            weightKg = weightKg,
            distanceKm = quote.distanceKm,
            priceMxn = quote.finalPriceMxn,
            isCustomQuote = quote.distanceKm > 25.0,
            notes = notes,
            payer = payer,
            paymentMethod = paymentMethod,
            isPaymentConfirmed = false,
            paymentConfirmedBy = null,
            status = "Esperando conductor",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = orderDao.insertOrder(newOrder)
        return newOrder.copy(id = id)
    }

    suspend fun confirmOrderPayment(orderId: Long, confirmedBy: String) {
        orderDao.updatePaymentConfirmation(orderId, confirmed = true, confirmedBy = confirmedBy)
    }

    suspend fun registerDriver(
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
        acceptedTermsVersion: String = "v1.0-2026-MX",
        locationPermissionGranted: Boolean = true
    ): DriverEntity {
        val newDriver = DriverEntity(
            fullName = fullName,
            phone = phone,
            vehicle = vehicle,
            zone = if (coverageZone.isNotEmpty()) coverageZone else zone,
            email = email,
            photoUri = photoUri,
            brand = brand,
            model = model,
            year = year,
            licensePlate = licensePlate,
            cargoCapacityKg = cargoCapacityKg,
            coverageZone = if (coverageZone.isNotEmpty()) coverageZone else zone,
            availableSchedule = availableSchedule,
            emergencyContactName = emergencyContactName,
            emergencyContactPhone = emergencyContactPhone,
            acceptedTermsVersion = acceptedTermsVersion,
            locationPermissionGranted = locationPermissionGranted,
            status = "PENDIENTE_APROBACION",
            isApproved = false,
            isAvailable = true
        )
        val id = driverDao.insertDriver(newDriver)
        return newDriver.copy(id = id)
    }

    suspend fun acceptOrder(orderId: Long, driver: DriverEntity) {
        orderDao.assignDriver(
            orderId = orderId,
            status = "Aceptado",
            driverId = driver.id,
            driverName = driver.fullName,
            driverPhone = driver.phone
        )
    }

    suspend fun updateOrderStatus(orderId: Long, status: String) {
        orderDao.updateOrderStatus(orderId, status)
    }

    suspend fun completeDelivery(orderId: Long, photoUri: String?, qrCode: String?) {
        val order = orderDao.getOrderById(orderId)
        orderDao.completeDelivery(orderId, photoUri, qrCode)
        if (order?.driverId != null) {
            driverDao.incrementDeliveries(order.driverId)
        }
    }

    suspend fun setDriverApproval(driverId: Long, approved: Boolean) {
        val statusStr = if (approved) "APROBADO" else "RECHAZADO"
        driverDao.setDriverApproval(driverId, approved, statusStr)
    }

    suspend fun setDriverStatus(driverId: Long, status: String) {
        val isApproved = status == "APROBADO"
        driverDao.setDriverStatus(driverId, status, isApproved)
    }

    suspend fun updateDriverLocation(orderId: Long, lat: Double, lng: Double) {
        orderDao.updateDriverLocation(orderId, lat, lng)
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentOrders = allOrders.firstOrNull() ?: emptyList()
        if (currentOrders.isEmpty()) {
            // Seed sample Drivers
            val driver1Id = driverDao.insertDriver(
                DriverEntity(
                    fullName = "Carlos Mendoza",
                    phone = "+52 55 1234 5678",
                    vehicle = "Motocicleta Electric 2024",
                    zone = "Centro",
                    isApproved = true,
                    isAvailable = true,
                    rating = 4.9f,
                    totalDeliveries = 142
                )
            )

            val driver2Id = driverDao.insertDriver(
                DriverEntity(
                    fullName = "Valeria Ramos",
                    phone = "+52 55 8765 4321",
                    vehicle = "Auto Sedan Hybrid",
                    zone = "Norte",
                    isApproved = true,
                    isAvailable = true,
                    rating = 4.8f,
                    totalDeliveries = 89
                )
            )

            // Pending Driver Application
            driverDao.insertDriver(
                DriverEntity(
                    fullName = "Jorge Luis Hernández",
                    phone = "+52 55 9988 7766",
                    vehicle = "Camioneta Ligera",
                    zone = "Sur",
                    isApproved = false,
                    isAvailable = true,
                    rating = 5.0f,
                    totalDeliveries = 0
                )
            )

            // Seed Sample Orders
            orderDao.insertOrder(
                OrderEntity(
                    trackingCode = "YAVA-58219",
                    clientName = "María Fernández (Boutique Reforma)",
                    clientPhone = "+52 55 4433 2211",
                    originAddress = "Av. Paseo de la Reforma 222, Juarez, CDMX",
                    destinationAddress = "Calle Durango 145, Roma Norte, CDMX",
                    packageType = "Paquete Pequeño",
                    weightKg = 2.5,
                    distanceKm = 4.2,
                    priceMxn = 50.0,
                    notes = "Entregar en recepción boutique piso 3",
                    status = "En camino",
                    driverId = driver1Id,
                    driverName = "Carlos Mendoza",
                    driverPhone = "+52 55 1234 5678",
                    driverLat = 19.4265,
                    driverLng = -99.1678,
                    createdAt = System.currentTimeMillis() - 1800000
                )
            )

            orderDao.insertOrder(
                OrderEntity(
                    trackingCode = "YAVA-94102",
                    clientName = "Roberto Gómez",
                    clientPhone = "+52 55 1122 3344",
                    originAddress = "Plaza Satélite, Naucalpan",
                    destinationAddress = "Polanco III Secc, Miguel Hidalgo, CDMX",
                    packageType = "Mediano",
                    weightKg = 8.0,
                    distanceKm = 12.8,
                    priceMxn = 130.0,
                    notes = "Caja frágil con electrónicos. Manejar con cuidado.",
                    status = "Esperando conductor",
                    createdAt = System.currentTimeMillis() - 600000
                )
            )

            orderDao.insertOrder(
                OrderEntity(
                    trackingCode = "YAVA-33108",
                    clientName = "Restaurante La Matilde",
                    clientPhone = "+52 55 7766 5544",
                    originAddress = "Av. Insurgentes Sur 1200, Del Valle",
                    destinationAddress = "Calle Coyoacán 310, Del Valle Centro",
                    packageType = "Documentos",
                    weightKg = 0.8,
                    distanceKm = 2.1,
                    priceMxn = 50.0,
                    notes = "Documentación contable en sobre cerrado.",
                    status = "Entregado",
                    driverId = driver2Id,
                    driverName = "Valeria Ramos",
                    driverPhone = "+52 55 8765 4321",
                    deliveryPhotoUri = "sample_photo_proof",
                    deliveryQrCode = "YAVA-33108-CONFIRMED",
                    createdAt = System.currentTimeMillis() - 7200000
                )
            )

            // Seed sample user
            userDao.insertUser(
                UserEntity(
                    name = "Administrador YaVa!",
                    phone = "+52 55 0000 1111",
                    email = "admin@yava.app",
                    role = "ADMIN",
                    zone = "Centro"
                )
            )
        }

        // Seed Company Config if not present
        if (companyConfig.firstOrNull() == null) {
            companyConfigDao.saveCompanyConfig(CompanyConfigEntity())
        }
    }
}
