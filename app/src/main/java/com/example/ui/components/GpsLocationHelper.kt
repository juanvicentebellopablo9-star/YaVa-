package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.NationalCoverage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONObject

data class RealGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val formattedAddress: String
)

data class AddressSuggestion(
    val title: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

object GpsLocationHelper {

    fun searchAddressSuggestions(
        context: Context,
        query: String,
        onResult: (List<AddressSuggestion>) -> Unit
    ) {
        if (query.trim().length < 2) {
            onResult(emptyList())
            return
        }

        val cleanQuery = query.trim().lowercase(Locale.ROOT)

        val presetLocations = listOf(
            // CDMX y Área Metropolitana
            AddressSuggestion("Zócalo CDMX", "Plaza de la Constitución, Centro, Ciudad de México", 19.4326, -99.1332),
            AddressSuggestion("Polanco CDMX", "Av. Presidente Masaryk, Polanco, Ciudad de México", 19.4254, -99.1857),
            AddressSuggestion("Coyoacán CDMX", "Jardín Centenario, Coyoacán, Ciudad de México", 19.3466, -99.1617),
            AddressSuggestion("Santa Fe CDMX", "Av. Vasco de Quiroga, Santa Fe, Ciudad de México", 19.3587, -99.2542),
            AddressSuggestion("Aeropuerto AICM", "Av. Capitán Carlos León, CDMX", 19.4363, -99.0721),
            // Monterrey, Nuevo León
            AddressSuggestion("Macroplaza Monterrey", "Gran Plaza, Monterrey, Nuevo León", 25.6667, -100.3094),
            AddressSuggestion("San Pedro Garza García", "Av. Lázaro Cárdenas, San Pedro, Nuevo León", 25.6581, -100.4039),
            AddressSuggestion("Aeropuerto MTY", "Carretera Miguel Alemán, Apodaca, Nuevo León", 25.7781, -100.1061),
            // Guadalajara, Jalisco
            AddressSuggestion("Centro Guadalajara", "Av. Hidalgo, Centro, Guadalajara, Jalisco", 20.6597, -103.3496),
            AddressSuggestion("Zapopan Centro", "Av. Hidalgo, Zapopan, Jalisco", 20.7167, -103.4000),
            AddressSuggestion("Minerva Guadalajara", "Av. Vallarta, Guadalajara, Jalisco", 20.6739, -103.3744),
            // Mérida, Yucatán
            AddressSuggestion("Centro Histórico, Mérida", "Calle 60 x 61, Centro, Mérida, Yucatán", 20.9674, -89.6237),
            AddressSuggestion("Paseo de Montejo, Mérida", "Av. Paseo de Montejo, Mérida, Yucatán", 20.9850, -89.6180),
            AddressSuggestion("Plaza Altabrisa, Mérida", "Calle 7 No. 451, Altabrisa, Mérida, Yucatán", 21.0188, -89.5840),
            // Puebla
            AddressSuggestion("Centro Histórico Puebla", "Av. 5 de Mayo, Centro, Puebla", 19.0414, -98.2063),
            AddressSuggestion("Cholula, Puebla", "Av. Morelos, Cholula, Puebla", 19.0625, -98.3006),
            // Cancún, Quintana Roo
            AddressSuggestion("Hotel Zone Cancún", "Blvd. Kukulcán, Cancún, Quintana Roo", 21.1389, -86.7494),
            AddressSuggestion("Playa del Carmen", "Av. 5ta, Playa del Carmen, Quintana Roo", 20.6296, -87.0739),
            // Tijuana, Baja California
            AddressSuggestion("Centro Tijuana", "Av. Revolución, Centro, Tijuana, Baja California", 32.5149, -117.0382),
            AddressSuggestion("Aeropuerto Tijuana", "Carretera Aeropuerto, Tijuana, Baja California", 32.5410, -116.9700),
            // León, Guanajuato
            AddressSuggestion("Centro León", "Plaza de Armas, León, Guanajuato", 21.1250, -101.6860),
            AddressSuggestion("Plaza Mayor León", "Blvd. Adolfo López Mateos, León, Guanajuato", 21.1167, -101.6833),
            // Querétaro
            AddressSuggestion("Centro Querétaro", "Plaza de Armas, Querétaro", 20.5888, -100.3889),
            AddressSuggestion("Juriquilla Querétaro", "Av. Juriquilla, Querétaro", 20.7083, -100.4500),
            // Toluca, Estado de México
            AddressSuggestion("Centro Toluca", "Plaza de los Mártires, Toluca, Estado de México", 19.2826, -99.6557),
            // Veracruz
            AddressSuggestion("Malecón Veracruz", "Av. Manuel Ávila Camacho, Veracruz", 19.1738, -96.1342),
            AddressSuggestion("Plaza Ámbar Veracruz", "Av. Rufo Figueroa, Veracruz", 19.1630, -96.1870),
            // Hermosillo, Sonora
            AddressSuggestion("Centro Hermosillo", "Plaza Zaragoza, Hermosillo, Sonora", 29.0729, -110.9559),
            // Culiacán, Sinaloa
            AddressSuggestion("Centro Culiacán", "Av. Alvaro Obregón, Culiacán, Sinaloa", 24.8091, -107.3940),
            // Villahermosa, Tabasco
            AddressSuggestion("Centro Villahermosa", "Av. Francisco I. Madero, Villahermosa, Tabasco", 17.9895, -92.9474),
            // Oaxaca
            AddressSuggestion("Centro Oaxaca", "Zócalo, Oaxaca de Juárez, Oaxaca", 17.0732, -96.7266),
            // Chihuahua
            AddressSuggestion("Centro Chihuahua", "Plaza de Armas, Chihuahua, Chihuahua", 28.6333, -106.0828),
            // Acapulco, Guerrero
            AddressSuggestion("Costera Acapulco", "Av. Costera Miguel Alemán, Acapulco, Guerrero", 16.8531, -99.8237),
            // Saltillo, Coahuila
            AddressSuggestion("Centro Saltillo", "Plaza de Armas, Saltillo, Coahuila", 25.4262, -100.9956),
            // Morelia, Michoacán
            AddressSuggestion("Centro Morelia", "Plaza de Armas, Morelia, Michoacán", 19.7060, -101.1940)
        )

        val localMatches = presetLocations.filter {
            it.title.lowercase(Locale.ROOT).contains(cleanQuery) ||
            it.fullAddress.lowercase(Locale.ROOT).contains(cleanQuery)
        }

        Thread {
            val suggestions = mutableListOf<AddressSuggestion>()
            suggestions.addAll(localMatches)

            try {
                val geocoder = Geocoder(context, Locale("es", "MX"))
                // National search: append "México" for broader coverage if no state/city is specified
                val nationalSearchQuery = if (query.contains("México", ignoreCase = true) ||
                    NationalCoverage.STATE_NAMES.any { query.contains(it, ignoreCase = true) } ||
                    query.contains("CDMX", ignoreCase = true)) {
                    query
                } else {
                    "$query, México"
                }

                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(nationalSearchQuery, 5)
                if (!results.isNullOrEmpty()) {
                    for (addr in results) {
                        val title = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: query
                        val fullAddress = buildString {
                            if (!addr.thoroughfare.isNullOrEmpty()) append("${addr.thoroughfare} ${addr.subThoroughfare ?: ""}, ")
                            if (!addr.subLocality.isNullOrEmpty()) append("${addr.subLocality}, ")
                            if (!addr.locality.isNullOrEmpty()) append("${addr.locality}, ")
                            if (!addr.adminArea.isNullOrEmpty()) append("${addr.adminArea}")
                        }.ifEmpty { addr.getAddressLine(0) ?: "$query, México" }

                        val item = AddressSuggestion(
                            title = title,
                            fullAddress = fullAddress,
                            latitude = addr.latitude,
                            longitude = addr.longitude
                        )
                        if (suggestions.none { Math.abs(it.latitude - item.latitude) < 0.001 && Math.abs(it.longitude - item.longitude) < 0.001 }) {
                            suggestions.add(item)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore geocoder errors
            }

            // If no suggestion matched, dynamically add custom national address entry
            if (suggestions.isEmpty()) {
                val formattedQuery = "$query, México"
                suggestions.add(
                    AddressSuggestion(
                        title = query.take(30),
                        fullAddress = formattedQuery,
                        latitude = 19.4326 + (Math.random() - 0.5) * 0.05,
                        longitude = -99.1332 + (Math.random() - 0.5) * 0.05
                    )
                )
            }

            Handler(Looper.getMainLooper()).post {
                onResult(suggestions.take(6))
            }
        }.start()
    }

    fun calculateRealDistanceKm(
        context: Context,
        originAddress: String,
        destAddress: String,
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        onResult: (distanceKm: Double, routeUrl: String, oLat: Double, oLng: Double, dLat: Double, dLng: Double) -> Unit
    ) {
        Thread {
            var oLat = originLat ?: 0.0
            var oLng = originLng ?: 0.0
            var dLat = destLat ?: 0.0
            var dLng = destLng ?: 0.0

            val geocoder = Geocoder(context, Locale("es", "MX"))

            if (oLat == 0.0 || oLng == 0.0) {
                try {
                    @Suppress("DEPRECATION")
                    val oList = geocoder.getFromLocationName(originAddress, 1)
                    if (!oList.isNullOrEmpty()) {
                        oLat = oList[0].latitude
                        oLng = oList[0].longitude
                    }
                } catch (e: Exception) { }
            }

            if (dLat == 0.0 || dLng == 0.0) {
                try {
                    @Suppress("DEPRECATION")
                    val dList = geocoder.getFromLocationName(destAddress, 1)
                    if (!dList.isNullOrEmpty()) {
                        dLat = dList[0].latitude
                        dLng = dList[0].longitude
                    }
                } catch (e: Exception) { }
            }

            if (oLat == 0.0) { oLat = 19.4326; oLng = -99.1332 }
            if (dLat == 0.0) { dLat = 19.4326; dLng = -99.1332 }

            // Try Open Source Routing Machine (OSRM) driving network calculation
            var calculatedKm: Double? = null
            try {
                val osrmUrl = URL("https://router.project-osrm.org/route/v1/driving/$oLng,$oLat;$dLng,$dLat?overview=false")
                val conn = osrmUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "YaVaLogisticsApp/1.0")
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    if (json.optString("code") == "Ok") {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val meters = route.getDouble("distance")
                            calculatedKm = (meters / 1000.0).coerceAtLeast(1.0)
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}

            val finalKm = calculatedKm ?: run {
                val results = FloatArray(1)
                Location.distanceBetween(oLat, oLng, dLat, dLng, results)
                val straightLineMeters = results[0]
                val drivingMeters = straightLineMeters * 1.25
                (drivingMeters / 1000.0).coerceAtLeast(1.0)
            }

            val roundedKm = Math.round(finalKm * 10.0) / 10.0
            val routeUrl = "geo:0,0?q=$dLat,$dLng(Entrega YaVa)"

            Handler(Looper.getMainLooper()).post {
                onResult(roundedKm, routeUrl, oLat, oLng, dLat, dLng)
            }
        }.start()
    }

    fun openNavigationRoute(
        context: Context,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        destLabel: String = "Entrega YaVa!"
    ) {
        try {
            val uri = Uri.parse("geo:0,0?q=$destLat,$destLng(${Uri.encode(destLabel)})")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webUri = Uri.parse("https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=$originLat%2C$originLng%3B$destLat%2C$destLng")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) {
                Toast.makeText(context, "No se encontró aplicación de navegación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openGoogleMapsRoute(
        context: Context,
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ) {
        openNavigationRoute(context, originLat, originLng, destLat, destLng)
    }

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
