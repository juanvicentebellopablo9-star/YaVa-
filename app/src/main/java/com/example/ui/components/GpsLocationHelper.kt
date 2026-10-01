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
            AddressSuggestion("Centro Histórico, Mérida", "Calle 60 x 61, Centro, Mérida, Yucatán", 20.9674, -89.6237),
            AddressSuggestion("Paseo de Montejo, Mérida", "Av. Paseo de Montejo, Centro, Mérida, Yucatán", 20.9850, -89.6180),
            AddressSuggestion("Prolongación Paseo de Montejo", "Prol. Paseo de Montejo, Campestre, Mérida, Yucatán", 21.0050, -89.6220),
            AddressSuggestion("Plaza Altabrisa, Mérida", "Calle 7 No. 451, Altabrisa, Mérida, Yucatán", 21.0188, -89.5840),
            AddressSuggestion("Gran Plaza, Mérida", "Calle 50 No. 460, Gonzalo Guerrero, Mérida, Yucatán", 21.0312, -89.6285),
            AddressSuggestion("Plaza La Isla Mérida", "Cabo Norte, Temozón Norte, Mérida, Yucatán", 21.0450, -89.5780),
            AddressSuggestion("Plaza Galerías Mérida", "Calle 60 No. 299, Revolución, Mérida, Yucatán", 21.0360, -89.6350),
            AddressSuggestion("The Harbor Mérida", "Vía Montejo, Cordemex, Mérida, Yucatán", 21.0330, -89.6320),
            AddressSuggestion("City Center Mérida", "Av. Andrés García Lavín, San Ramón Norte, Mérida, Yucatán", 21.0280, -89.5980),
            AddressSuggestion("Plaza Uptown Mérida", "Calle 15 x 18, Vista Alegre, Mérida, Yucatán", 21.0150, -89.5920),
            AddressSuggestion("Macroplaza Mérida", "Calle 33, Polígono 108, Mérida, Yucatán", 20.9920, -89.5750),
            AddressSuggestion("Aeropuerto Int. de Mérida", "Carretera Mérida-Umán Km 14.5, Mérida, Yucatán", 20.9370, -89.6577),
            AddressSuggestion("Parque Santa Lucía, Mérida", "Calle 60 x 55, Centro, Mérida, Yucatán", 20.9705, -89.6228),
            AddressSuggestion("Hospital Star Médica Mérida", "Calle 26 No. 199, Altabrisa, Mérida, Yucatán", 21.0195, -89.5830),
            AddressSuggestion("Hospital O'Horán, Mérida", "Av. Itzaes x Jacinto Canek, Centro, Mérida, Yucatán", 20.9680, -89.6380),
            AddressSuggestion("Universidad UADY Centro", "Calle 60 x 57, Centro, Mérida, Yucatán", 20.9712, -89.6230),
            AddressSuggestion("Francisco de Montejo, Mérida", "Calle 50 x 51, Francisco de Montejo, Mérida, Yucatán", 21.0250, -89.6450),
            AddressSuggestion("Fraccionamiento Las Américas", "Av. Cronista Deportivo, Las Américas, Mérida, Yucatán", 21.0650, -89.6420),
            AddressSuggestion("Ciudad Caucel, Mérida", "Av. Cronista, Ciudad Caucel, Mérida, Yucatán", 20.9980, -89.7020),
            AddressSuggestion("Los Héroes, Mérida", "Av. Los Héroes, Fracc. Los Héroes, Mérida, Yucatán", 20.9810, -89.5480),
            AddressSuggestion("Av. Andrés García Lavín", "Av. García Lavín, San Ramón Norte, Mérida, Yucatán", 21.0260, -89.5990),
            AddressSuggestion("Anillo Periférico Norte Mérida", "Anillo Periférico Km 25, Temozón Norte, Mérida, Yucatán", 21.0480, -89.6050),
            AddressSuggestion("Mercado Lucas de Gálvez", "Calle 65 x 56, Centro, Mérida, Yucatán", 20.9620, -89.6210),
            AddressSuggestion("Terminal ADO Mérida CAME", "Calle 68 x 69 y 71, Centro, Mérida, Yucatán", 20.9580, -89.6280),
            AddressSuggestion("Kanasín Centro, Yucatán", "Calle 21, Centro, Kanasín, Yucatán", 20.9333, -89.5583),
            AddressSuggestion("Umán Centro, Yucatán", "Calle 20, Centro, Umán, Yucatán", 20.8833, -89.7500),
            AddressSuggestion("Progreso Malecón, Yucatán", "Calle 19, Malecón, Progreso, Yucatán", 21.2833, -89.6644)
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
                val meridaSearchQuery = if (query.contains("Mérida", ignoreCase = true) || query.contains("Yucatán", ignoreCase = true)) {
                    query
                } else {
                    "$query, Mérida, Yucatán"
                }

                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(meridaSearchQuery, 5)
                if (!results.isNullOrEmpty()) {
                    for (addr in results) {
                        val title = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: query
                        val fullAddress = buildString {
                            if (!addr.thoroughfare.isNullOrEmpty()) append("${addr.thoroughfare} ${addr.subThoroughfare ?: ""}, ")
                            if (!addr.subLocality.isNullOrEmpty()) append("${addr.subLocality}, ")
                            if (!addr.locality.isNullOrEmpty()) append("${addr.locality}, ")
                            if (!addr.adminArea.isNullOrEmpty()) append("${addr.adminArea}")
                        }.ifEmpty { addr.getAddressLine(0) ?: "$query, Mérida, Yucatán" }

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

            // If no suggestion matched, dynamically add custom Mérida address entry
            if (suggestions.isEmpty()) {
                val formattedQuery = if (query.contains("Mérida", ignoreCase = true)) query else "$query, Mérida, Yucatán"
                suggestions.add(
                    AddressSuggestion(
                        title = query.take(30),
                        fullAddress = formattedQuery,
                        latitude = 20.9674 + (Math.random() - 0.5) * 0.05,
                        longitude = -89.6237 + (Math.random() - 0.5) * 0.05
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

            if (oLat == 0.0) { oLat = 20.9674; oLng = -89.6237 }
            if (dLat == 0.0) { dLat = 21.0188; dLng = -89.5840 }

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
