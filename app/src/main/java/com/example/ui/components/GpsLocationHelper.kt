package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

data class RealGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val formattedAddress: String
)

object GpsLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentRealGpsLocation(
        context: Context,
        onLocationReceived: (RealGpsLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Permisos de ubicación GPS no otorgados. Otorga permisos en la app para precisión en tiempo real.")
            return
        }

        try {
            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val address = resolveAddress(context, location.latitude, location.longitude)
                    onLocationReceived(
                        RealGpsLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            formattedAddress = address
                        )
                    )
                } else {
                    // Try lastLocation cache or fallback to system LocationManager
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            val address = resolveAddress(context, lastLoc.latitude, lastLoc.longitude)
                            onLocationReceived(
                                RealGpsLocation(
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude,
                                    accuracyMeters = lastLoc.accuracy,
                                    formattedAddress = address
                                )
                            )
                        } else {
                            fetchFallbackSystemLocationManager(context, onLocationReceived, onError)
                        }
                    }.addOnFailureListener {
                        fetchFallbackSystemLocationManager(context, onLocationReceived, onError)
                    }
                }
            }.addOnFailureListener {
                fetchFallbackSystemLocationManager(context, onLocationReceived, onError)
            }
        } catch (e: Exception) {
            fetchFallbackSystemLocationManager(context, onLocationReceived, onError)
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealTimeLocationUpdates(
        context: Context,
        intervalMs: Long = 4000L,
        onLocationUpdate: (RealGpsLocation) -> Unit,
        onError: (String) -> Unit
    ): LocationCallback? {
        if (!hasLocationPermission(context)) {
            onError("Permisos de ubicación GPS no otorgados para rastreo en tiempo real.")
            return null
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        val address = resolveAddress(context, loc.latitude, loc.longitude)
                        onLocationUpdate(
                            RealGpsLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracyMeters = loc.accuracy,
                                formattedAddress = address
                            )
                        )
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            callback
        } catch (e: Exception) {
            onError("Error iniciando rastreo GPS en tiempo real: ${e.message}")
            null
        }
    }

    fun stopRealTimeLocationUpdates(context: Context, callback: LocationCallback) {
        try {
            LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(callback)
        } catch (e: Exception) {
            // Ignore cleanup error
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFallbackSystemLocationManager(
        context: Context,
        onLocationReceived: (RealGpsLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Servicio de GPS del dispositivo no disponible.")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onError("El GPS o Servicios de Ubicación están desactivados. Por favor actívalos en ajustes.")
            return
        }

        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

        try {
            val lastKnown = locationManager.getLastKnownLocation(provider)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null && (System.currentTimeMillis() - lastKnown.time) < 60000) {
                val address = resolveAddress(context, lastKnown.latitude, lastKnown.longitude)
                onLocationReceived(
                    RealGpsLocation(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        accuracyMeters = lastKnown.accuracy,
                        formattedAddress = address
                    )
                )
                return
            }

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    val address = resolveAddress(context, location.latitude, location.longitude)
                    onLocationReceived(
                        RealGpsLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            formattedAddress = address
                        )
                    )
                }
                @Deprecated("Deprecated in API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper())

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (e: Exception) { }
                if (lastKnown != null) {
                    val address = resolveAddress(context, lastKnown.latitude, lastKnown.longitude)
                    onLocationReceived(
                        RealGpsLocation(
                            latitude = lastKnown.latitude,
                            longitude = lastKnown.longitude,
                            accuracyMeters = lastKnown.accuracy,
                            formattedAddress = address
                        )
                    )
                } else {
                    onError("No se obtuvo fijación GPS a tiempo. Reintenta al aire libre.")
                }
            }, 5000)
        } catch (e: Exception) {
            onError("Error obteniendo ubicación GPS real: ${e.message}")
        }
    }

    private fun resolveAddress(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("es", "MX"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val thoroughfare = addr.thoroughfare ?: "Calle detectada"
                val subThoroughfare = addr.subThoroughfare ?: ""
                val subLocality = addr.subLocality ?: addr.locality ?: "Centro"
                val city = addr.locality ?: addr.adminArea ?: "CDMX"
                "$thoroughfare $subThoroughfare, $subLocality, $city (GPS Exacto: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
            } else {
                "Ubicación GPS: Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}"
            }
        } catch (e: Exception) {
            "Ubicación GPS Real (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
        }
    }
}
