package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE trackingCode = :code LIMIT 1")
    suspend fun getOrderByTrackingCode(code: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE trackingCode = :code LIMIT 1")
    fun observeOrderByTrackingCode(code: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE clientPhone = :phone ORDER BY createdAt DESC")
    fun getOrdersByClientPhone(phone: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = 'Esperando conductor' ORDER BY createdAt DESC")
    fun getAvailableOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE driverId = :driverId ORDER BY updatedAt DESC")
    fun getOrdersByDriver(driverId: Long): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, driverId = :driverId, driverName = :driverName, driverPhone = :driverPhone, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun assignDriver(orderId: Long, status: String, driverId: Long, driverName: String, driverPhone: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET deliveryPhotoUri = :photoUri, deliveryQrCode = :qrCode, status = 'Entregado', updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun completeDelivery(orderId: Long, photoUri: String?, qrCode: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET driverLat = :lat, driverLng = :lng, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateDriverLocation(orderId: Long, lat: Double, lng: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET isPaymentConfirmed = :confirmed, paymentConfirmedBy = :confirmedBy, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updatePaymentConfirmation(orderId: Long, confirmed: Boolean, confirmedBy: String, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY createdAt DESC")
    fun getAllDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE isApproved = 0 ORDER BY createdAt DESC")
    fun getPendingDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE isApproved = 1 AND isAvailable = 1 ORDER BY rating DESC")
    fun getApprovedAvailableDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE id = :id")
    suspend fun getDriverById(id: Long): DriverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity): Long

    @Update
    suspend fun updateDriver(driver: DriverEntity)

    @Query("UPDATE drivers SET isApproved = :isApproved, status = :status WHERE id = :driverId")
    suspend fun setDriverApproval(driverId: Long, isApproved: Boolean, status: String = if (isApproved) "APROBADO" else "RECHAZADO")

    @Query("UPDATE drivers SET status = :status, isApproved = :isApproved WHERE id = :driverId")
    suspend fun setDriverStatus(driverId: Long, status: String, isApproved: Boolean)

    @Query("UPDATE drivers SET isAvailable = :isAvailable WHERE id = :driverId")
    suspend fun setDriverAvailability(driverId: Long, isAvailable: Boolean)

    @Query("UPDATE drivers SET totalDeliveries = totalDeliveries + 1 WHERE id = :driverId")
    suspend fun incrementDeliveries(driverId: Long)
}

@Dao
interface CompanyConfigDao {
    @Query("SELECT * FROM company_config WHERE id = 1 LIMIT 1")
    fun getCompanyConfig(): Flow<CompanyConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompanyConfig(config: CompanyConfigEntity)
}

@Dao
interface LegalConsentDao {
    @Query("SELECT * FROM legal_consents ORDER BY acceptedAt DESC")
    fun getAllLegalConsents(): Flow<List<LegalConsentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegalConsent(consent: LegalConsentEntity): Long
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
}
