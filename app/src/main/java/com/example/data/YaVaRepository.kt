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

    suspend fun getOrderById(id: Long): OrderEntity? = orderDao.getOrderById(id)

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
        originState: String = "",
        destState: String = "",
        originLat: Double = 0.0,
        originLng: Double = 0.0,
        destLat: Double = 0.0,
        destLng: Double = 0.0,
        isHighDemand: Boolean = false,
        isWeatherSurge: Boolean = false
    ): OrderEntity {
        val quote = PricingCalculator.calculateQuote(
            distanceKm = distanceKm,
            packageType = packageType,
            weightKg = weightKg,
            isHighDemand = isHighDemand,
            isWeatherSurge = isWeatherSurge
        )
        val trackingCode = "YAVA-${Random.nextInt(10000, 99999)}"
        val tier = NationalCoverage.determineServiceTier(quote.distanceKm)

        val newOrder = OrderEntity(
            trackingCode = trackingCode,
            clientName = clientName,
            clientPhone = clientPhone,
            originAddress = originAddress,
            destinationAddress = destinationAddress,
            originState = originState,
            destState = destState,
            originLat = originLat,
            originLng = originLng,
            destLat = destLat,
            destLng = destLng,
            packageType = packageType,
            weightKg = weightKg,
            distanceKm = quote.distanceKm,
            estimatedTimeMinutes = quote.estimatedTimeMinutes,
            priceMxn = quote.finalPriceMxn,
            baseFareMxn = quote.baseFareMxn,
            distanceFareMxn = quote.distanceFareMxn,
            timeFareMxn = quote.timeFareMxn,
            surgeMultiplier = quote.surgeMultiplier,
            platformCommissionMxn = quote.platformCommissionAmountMxn,
            driverEarningsMxn = quote.driverEarningsMxn,
            isCustomQuote = quote.distanceKm > 25.0,
            serviceTier = tier.id,
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
        // Only seed Company Configuration if not present.
        // NO mock orders, NO dummy drivers, NO fake users are seeded into production flow.
        if (companyConfig.firstOrNull() == null) {
            companyConfigDao.saveCompanyConfig(CompanyConfigEntity())
        }
    }
}
