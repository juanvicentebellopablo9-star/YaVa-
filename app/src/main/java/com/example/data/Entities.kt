package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackingCode: String,
    val clientName: String,
    val clientPhone: String,
    val originAddress: String,
    val destinationAddress: String,
    val originState: String = "",
    val destState: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val packageType: String, // Documentos, Paquete Pequeño, Mediano, Pesado (hasta 20kg)
    val weightKg: Double,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int = 15,
    val priceMxn: Double,
    val baseFareMxn: Double = 22.0,
    val distanceFareMxn: Double = 0.0,
    val timeFareMxn: Double = 0.0,
    val surgeMultiplier: Double = 1.0,
    val platformCommissionMxn: Double = 0.0,
    val driverEarningsMxn: Double = 0.0,
    val isCustomQuote: Boolean = false,
    val serviceTier: String = "LOCAL", // LOCAL, INTERCITY, NATIONAL
    val notes: String = "",
    val payer: String = "Paga quien envía", // "Paga quien envía" or "Paga quien recibe"
    val paymentMethod: String = "EFECTIVO", // "EFECTIVO", "TERMINAL", "TRANSFERENCIA"
    val isPaymentConfirmed: Boolean = false,
    val paymentConfirmedBy: String? = null, // "DIRECTOR", "CONDUCTOR"
    val status: String, // Creado, Esperando conductor, Aceptado, En camino, Entregado, Cancelado
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverLat: Double = 20.9674,
    val driverLng: Double = -89.6237,
    val deliveryPhotoUri: String? = null,
    val deliveryQrCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val vehicle: String, // Motocicleta, Auto Sedan, Camioneta, Bicicleta Eléctrica
    val zone: String, // Estado de la República Mexicana
    val email: String = "socio@yava.app",
    val photoUri: String? = null,
    val brand: String = "Italika / Honda",
    val model: String = "125cc / Standard",
    val year: Int = 2024,
    val licensePlate: String = "YAV-100",
    val cargoCapacityKg: Double = 25.0,
    val coverageZone: String = "Cobertura Nacional",
    val availableSchedule: String = "Tiempo Completo (8 AM - 8 PM)",
    val emergencyContactName: String = "Contacto de Emergencia",
    val emergencyContactPhone: String = "9990000000",
    val acceptedTermsVersion: String = "v1.0-2026-MX",
    val locationPermissionGranted: Boolean = true,
    val status: String = "PENDIENTE_APROBACION", // PENDIENTE_APROBACION, APROBADO, RECHAZADO, SUSPENDIDO
    val isApproved: Boolean = false,
    val isAvailable: Boolean = true,
    val rating: Float = 5.0f,
    val totalDeliveries: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val role: String, // CLIENTE, CONDUCTOR, ADMIN
    val zone: String = "Cobertura Nacional"
)

@Entity(tableName = "company_config")
data class CompanyConfigEntity(
    @PrimaryKey val id: Long = 1,
    val whatsappNumber: String = "529997431941",
    val phoneNumber: String = "9997431941",
    val supportEmail: String = "yavaenvios@gmail.com",
    val facebookName: String = "YaVa Envíos",
    val facebookUrl: String = "https://www.facebook.com/yavaenvios",
    val operatingHours: String = "Lunes a Sábado: 8:00 AM - 8:00 PM",
    val emergencyNotice: String = "Soporte activo 24/7 para entregas en curso.",
    val autoQuoteMsg: String = "Hola YaVa!, quiero información o solicitar una cotización de envío.",
    val autoDriverSupportMsg: String = "Hola YaVa!, soy Socio Conductor y necesito soporte en ruta.",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "legal_consents")
data class LegalConsentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val userEmail: String,
    val userPhone: String,
    val userRole: String, // CLIENTE, CONDUCTOR, NEGOCIO
    val termsAccepted: Boolean = true,
    val privacyAccepted: Boolean = true,
    val documentVersion: String = "v1.0-2026-MX",
    val acceptedAt: Long = System.currentTimeMillis(),
    val deviceInfo: String = "Android Client (YaVa App)"
)
