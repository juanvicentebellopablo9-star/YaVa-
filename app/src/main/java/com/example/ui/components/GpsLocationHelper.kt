package com.example.ui.components

import android.Manifest
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

    fun getCurrentRealGpsLocation(
        context: Context,
        onLocationReceived: (RealGpsLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Permisos de ubicación GPS no otorgados. Otorga permisos en la app para precisión en tiempo real.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Servicio de GPS del dispositivo no disponible.")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onError("El GPS o Servicios de Ubicación están desactivados. Por favor actívalos en los ajustes de tu dispositivo.")
            return
        }

        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

        try {
            // Check last known location first for immediate response
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

            // Single update request
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

            // Fallback timeout in 5 seconds if single update doesn't return
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (e: Exception) {
                    // Ignore cleanup exception
                }
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

        } catch (e: SecurityException) {
            onError("Error de seguridad al acceder al sensor GPS: ${e.message}")
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
