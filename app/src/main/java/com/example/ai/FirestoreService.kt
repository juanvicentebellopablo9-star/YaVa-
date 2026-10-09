package com.example.ai

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore Data Synchronization for YaVa! Logistics
 * Provides real-time document snapshot listeners, cloud storage for orders,
 * delivery progress tracking for senders, and driver telemetry.
 */
data class CloudOrder(
    val trackingCode: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val originAddress: String = "",
    val destinationAddress: String = "",
    val originState: String = "",
    val destState: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val distanceKm: Double = 0.0,
    val priceMxn: Double = 0.0,
    val packageType: String = "Paquete Estándar",
    val serviceTier: String = "LOCAL", // LOCAL, INTERCITY, NATIONAL
    val status: String = "Creado", // Creado, Esperando conductor, Aceptado, En camino, Entregado, Cancelado
    val progressPercent: Int = 15,
    val statusDescription: String = "Pedido registrado y confirmado en la nube",
    val estimatedArrivalMinutes: Int = 15,
    val paymentMethod: String = "EFECTIVO",
    val isPaymentConfirmed: Boolean = false,
    val payer: String = "Paga quien envía",
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverVehicle: String? = null,
    val driverLat: Double? = 19.4326,
    val driverLng: Double? = -99.1332,
    val deliveryPhotoUri: String? = null,
    val deliveryQrCode: String? = null,
    val deliveryPin: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreSnapshotMetadata(
    val source: String = "FIRESTORE_SERVER", // FIRESTORE_SERVER, FIRESTORE_CACHE, LOCAL_FALLBACK
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val snapshotTimestamp: Long = System.currentTimeMillis(),
    val snapshotVersion: Long = 1L
)

data class FirestoreDeliverySnapshot(
    val order: CloudOrder,
    val metadata: FirestoreSnapshotMetadata = FirestoreSnapshotMetadata()
)

data class CloudChatMessage(
    val id: String = "",
    val sender: String = "user", // "user" or "gemini"
    val message: String = "",
    val modelUsed: String = "gemini-3.5-flash",
    val timestamp: Long = System.currentTimeMillis()
)

object FirestoreService {
    private const val TAG = "FirestoreService"
    private const val ORDERS_COLLECTION = "yava_orders"

    // In-memory real-time mirror for ultra-fast local updates and offline resilience
    private val inMemoryOrders = MutableStateFlow<Map<String, CloudOrder>>(emptyMap())
    private var snapshotCounter = 0L

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Default FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.yava.logistics")
                    .setProjectId("yava-logistics")
                    .setApiKey("AIzaSyLocalSandboxDevKey987654321")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "FirebaseApp programmatically initialized for Firestore")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase programmatic initialization note: ${e.message}")
        }
    }

    /**
     * Real-time Firestore snapshot listener for a specific order.
     * Listens to cloud document changes and emits FirestoreDeliverySnapshot whenever
     * the driver location, status, or progress updates in Firestore.
     */
    fun observeOrderSnapshot(trackingCode: String): Flow<FirestoreDeliverySnapshot?> = callbackFlow {
        if (trackingCode.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val db = firestore
        if (db != null) {
            try {
                val docRef = db.collection(ORDERS_COLLECTION).document(trackingCode)
                val listener = docRef.addSnapshotListener { snapshot, error ->
                    snapshotCounter++
                    if (error != null) {
                        Log.w(TAG, "Firestore snapshot error: ${error.message}. Checking local mirror.")
                        val fallback = inMemoryOrders.value[trackingCode]
                        if (fallback != null) {
                            trySend(
                                FirestoreDeliverySnapshot(
                                    order = fallback,
                                    metadata = FirestoreSnapshotMetadata(
                                        source = "LOCAL_REACTIVE_CACHE",
                                        isFromCache = true,
                                        snapshotTimestamp = System.currentTimeMillis(),
                                        snapshotVersion = snapshotCounter
                                    )
                                )
                            )
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val cloudOrder = snapshot.toObject(CloudOrder::class.java)
                        if (cloudOrder != null) {
                            // Update local mirror as well
                            val currentMap = inMemoryOrders.value.toMutableMap()
                            currentMap[trackingCode] = cloudOrder
                            inMemoryOrders.value = currentMap

                            val isCache = snapshot.metadata.isFromCache
                            val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                            val meta = FirestoreSnapshotMetadata(
                                source = if (isCache) "FIRESTORE_CACHE" else "FIRESTORE_SERVER",
                                isFromCache = isCache,
                                hasPendingWrites = hasPendingWrites,
                                snapshotTimestamp = System.currentTimeMillis(),
                                snapshotVersion = snapshotCounter
                            )
                            trySend(FirestoreDeliverySnapshot(order = cloudOrder, metadata = meta))
                            return@addSnapshotListener
                        }
                    }

                    // If doc not found in Firestore yet, try local mirror
                    val local = inMemoryOrders.value[trackingCode]
                    if (local != null) {
                        trySend(
                            FirestoreDeliverySnapshot(
                                order = local,
                                metadata = FirestoreSnapshotMetadata(
                                    source = "FIRESTORE_LOCAL_PENDING",
                                    isFromCache = true,
                                    hasPendingWrites = true,
                                    snapshotTimestamp = System.currentTimeMillis(),
                                    snapshotVersion = snapshotCounter
                                )
                            )
                        )
                    }
                }

                awaitClose { listener.remove() }
                return@callbackFlow
            } catch (e: Exception) {
                Log.w(TAG, "Failed to attach Firestore snapshot listener: ${e.message}")
            }
        }

        // Fallback reactive listener if Firestore native client is not reachable
        val local = inMemoryOrders.value[trackingCode]
        if (local != null) {
            trySend(
                FirestoreDeliverySnapshot(
                    order = local,
                    metadata = FirestoreSnapshotMetadata(
                        source = "OFFLINE_SNAPSHOT_ENGINE",
                        isFromCache = true,
                        snapshotTimestamp = System.currentTimeMillis(),
                        snapshotVersion = ++snapshotCounter
                    )
                )
            )
        } else {
            trySend(null)
        }
        awaitClose { }
    }

    /**
     * Publishes or updates an order in Firestore collection "yava_orders".
     */
    suspend fun syncOrderToCloud(order: CloudOrder): Boolean {
        // Always update in-memory mirror immediately for zero latency
        val currentMap = inMemoryOrders.value.toMutableMap()
        currentMap[order.trackingCode] = order
        inMemoryOrders.value = currentMap

        val db = firestore ?: return true
        return try {
            db.collection(ORDERS_COLLECTION)
                .document(order.trackingCode)
                .set(order)
                .await()
            Log.d(TAG, "Order ${order.trackingCode} successfully synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed, local mirror preserved: ${e.message}")
            false
        }
    }

    /**
     * Dedicated method to advance delivery progress in Firestore,
     * triggering real-time snapshot listeners for the sender.
     */
    suspend fun updateDeliveryProgress(
        trackingCode: String,
        status: String,
        progressPercent: Int,
        statusDescription: String,
        driverLat: Double? = null,
        driverLng: Double? = null,
        driverName: String? = null,
        estimatedArrivalMinutes: Int? = null
    ): Boolean {
        val existing = inMemoryOrders.value[trackingCode] ?: CloudOrder(trackingCode = trackingCode)
        val updated = existing.copy(
            status = status,
            progressPercent = progressPercent,
            statusDescription = statusDescription,
            driverLat = driverLat ?: existing.driverLat,
            driverLng = driverLng ?: existing.driverLng,
            driverName = driverName ?: existing.driverName,
            estimatedArrivalMinutes = estimatedArrivalMinutes ?: existing.estimatedArrivalMinutes,
            updatedAt = System.currentTimeMillis()
        )

        return syncOrderToCloud(updated)
    }

    suspend fun saveChatMessage(userId: String, chatMessage: CloudChatMessage): Boolean {
        return try {
            val db = firestore ?: return false
            db.collection("users")
                .document(userId)
                .collection("gemini_chat_history")
                .add(chatMessage)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun observeCloudOrders(): Flow<List<CloudOrder>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(inMemoryOrders.value.values.toList())
            close()
            return@callbackFlow
        }

        val listener = db.collection(ORDERS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(inMemoryOrders.value.values.toList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(CloudOrder::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }
}

