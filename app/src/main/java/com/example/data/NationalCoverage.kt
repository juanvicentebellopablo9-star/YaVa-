package com.example.data

/**
 * YaVa! National Coverage — Catálogo de estados y ciudades de México
 *
 * Define la cobertura nacional de la plataforma con los 32 estados,
 * sus ciudades principales y coordenadas GPS para cálculo de rutas.
 */

data class MexicanState(
    val name: String,
    val code: String,
    val cities: List<CityCoordinate>
)

data class CityCoordinate(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class ServiceTier(
    val id: String,
    val label: String,
    val minDistanceKm: Double,
    val maxDistanceKm: Double,
    val ratePerKmMxn: Double,
    val ratePerMinuteMxn: Double,
    val baseFareMxn: Double,
    val estimatedSpeedKmh: Double,
    val description: String
)

object NationalCoverage {

    /** Todos los estados de México con sus ciudades principales */
    val MEXICAN_STATES: List<MexicanState> = listOf(
        MexicanState("Aguascalientes", "AGS", listOf(
            CityCoordinate("Aguascalientes", 21.8853, -102.2916),
            CityCoordinate("Jesús María", 21.9625, -102.3497),
            CityCoordinate("Rincón de Romos", 22.2286, -102.3197)
        )),
        MexicanState("Baja California", "BC", listOf(
            CityCoordinate("Tijuana", 32.5149, -117.0382),
            CityCoordinate("Mexicali", 32.6278, -115.4544),
            CityCoordinate("Ensenada", 31.8664, -116.5964),
            CityCoordinate("Rosarito", 32.3634, -117.0544)
        )),
        MexicanState("Baja California Sur", "BCS", listOf(
            CityCoordinate("La Paz", 24.1426, -110.3055),
            CityCoordinate("Los Cabos", 23.0538, -109.6800),
            CityCoordinate("Loreto", 26.0119, -111.3492)
        )),
        MexicanState("Campeche", "CAMP", listOf(
            CityCoordinate("San Francisco de Campeche", 19.8466, -90.5322),
            CityCoordinate("Ciudad del Carmen", 18.6539, -91.8239),
            CityCoordinate("Champotón", 19.3547, -90.7225)
        )),
        MexicanState("Chiapas", "CHIS", listOf(
            CityCoordinate("Tuxtla Gutiérrez", 16.7537, -93.0946),
            CityCoordinate("San Cristóbal de las Casas", 16.7373, -92.6376),
            CityCoordinate("Tapachula", 14.9008, -92.2886),
            CityCoordinate("Comitán", 16.2472, -92.1289)
        )),
        MexicanState("Chihuahua", "CHIH", listOf(
            CityCoordinate("Chihuahua", 28.6333, -106.0828),
            CityCoordinate("Ciudad Juárez", 31.6904, -106.4245),
            CityCoordinate("Delicias", 28.1889, -105.4647),
            CityCoordinate("Parral", 26.9296, -105.4236)
        )),
        MexicanState("Ciudad de México", "CDMX", listOf(
            CityCoordinate("CDMX Centro", 19.4326, -99.1332),
            CityCoordinate("Polanco", 19.4254, -99.1857),
            CityCoordinate("Coyoacán", 19.3466, -99.1617),
            CityCoordinate("Iztapalapa", 19.3563, -99.0647),
            CityCoordinate("Tlalpan", 19.2945, -99.1673)
        )),
        MexicanState("Coahuila", "COAH", listOf(
            CityCoordinate("Saltillo", 25.4262, -100.9956),
            CityCoordinate("Torreón", 25.5424, -103.4230),
            CityCoordinate("Monclova", 26.9013, -101.4211),
            CityCoordinate("Piedras Negras", 28.7118, -100.5239)
        )),
        MexicanState("Colima", "COL", listOf(
            CityCoordinate("Colima", 19.2410, -103.7274),
            CityCoordinate("Manzanillo", 19.0514, -104.3192),
            CityCoordinate("Villa de Álvarez", 19.2664, -103.7356)
        )),
        MexicanState("Durango", "DGO", listOf(
            CityCoordinate("Durango", 24.0277, -104.6542),
            CityCoordinate("Gómez Palacio", 25.5689, -103.4922),
            CityCoordinate("Lerdo", 25.5408, -103.5236)
        )),
        MexicanState("Estado de México", "EDOMEX", listOf(
            CityCoordinate("Toluca", 19.2826, -99.6557),
            CityCoordinate("Ecatepec", 19.6018, -99.0127),
            CityCoordinate("Nezahualcóyotl", 19.4152, -99.0292),
            CityCoordinate("Naucalpan", 19.4398, -99.2598),
            CityCoordinate("Tultitlán", 19.6486, -99.1689)
        )),
        MexicanState("Guanajuato", "GTO", listOf(
            CityCoordinate("León", 21.1250, -101.6860),
            CityCoordinate("Guanajuato", 21.0177, -101.2591),
            CityCoordinate("Irapuato", 20.6786, -101.3556),
            CityCoordinate("Celaya", 20.5228, -100.8167)
        )),
        MexicanState("Guerrero", "GRO", listOf(
            CityCoordinate("Chilpancingo", 17.5538, -99.5014),
            CityCoordinate("Acapulco", 16.8531, -99.8237),
            CityCoordinate("Iguala", 18.3536, -99.5394),
            CityCoordinate("Taxco", 18.5561, -99.5264)
        )),
        MexicanState("Hidalgo", "HGO", listOf(
            CityCoordinate("Pachuca", 20.1011, -98.7591),
            CityCoordinate("Tulancingo", 20.0876, -98.3689),
            CityCoordinate("Ixmiquilpan", 20.3911, -99.5094)
        )),
        MexicanState("Jalisco", "JAL", listOf(
            CityCoordinate("Guadalajara", 20.6597, -103.3496),
            CityCoordinate("Zapopan", 20.7167, -103.4000),
            CityCoordinate("Tlaquepaque", 20.6419, -103.2956),
            CityCoordinate("Puerto Vallarta", 20.6534, -105.2253),
            CityCoordinate("Lagos de Moreno", 21.3575, -101.9389)
        )),
        MexicanState("Michoacán", "MICH", listOf(
            CityCoordinate("Morelia", 19.7060, -101.1940),
            CityCoordinate("Uruapan", 19.4119, -102.0556),
            CityCoordinate("Lázaro Cárdenas", 17.9592, -102.2014),
            CityCoordinate("Pátzcuaro", 19.5131, -101.6089)
        )),
        MexicanState("Morelos", "MOR", listOf(
            CityCoordinate("Cuernavaca", 18.9241, -99.2216),
            CityCoordinate("Jiutepec", 18.8814, -99.1669),
            CityCoordinate("Cuautla", 18.8123, -98.9522)
        )),
        MexicanState("Nayarit", "NAY", listOf(
            CityCoordinate("Tepic", 21.5085, -104.8939),
            CityCoordinate("Bahía de Banderas", 20.7636, -105.4364),
            CityCoordinate("Compostela", 21.1750, -105.1833)
        )),
        MexicanState("Nuevo León", "NL", listOf(
            CityCoordinate("Monterrey", 25.6866, -100.3161),
            CityCoordinate("Guadalupe", 25.6791, -100.2578),
            CityCoordinate("San Pedro Garza García", 25.6581, -100.4039),
            CityCoordinate("Apodaca", 25.7811, -100.2314),
            CityCoordinate("Escobedo", 25.8275, -100.3244)
        )),
        MexicanState("Oaxaca", "OAX", listOf(
            CityCoordinate("Oaxaca de Juárez", 17.0732, -96.7266),
            CityCoordinate("Salina Cruz", 16.1711, -95.2033),
            CityCoordinate("Juchitán", 16.4333, -95.0167),
            CityCoordinate("Puerto Escondido", 15.8700, -97.0767)
        )),
        MexicanState("Puebla", "PUE", listOf(
            CityCoordinate("Puebla", 19.0414, -98.2063),
            CityCoordinate("Tehuacán", 18.4647, -97.3936),
            CityCoordinate("Cholula", 19.0625, -98.3006),
            CityCoordinate("Atlixco", 18.9142, -98.4294)
        )),
        MexicanState("Querétaro", "QRO", listOf(
            CityCoordinate("Querétaro", 20.5888, -100.3889),
            CityCoordinate("San Juan del Río", 20.3894, -100.0000),
            CityCoordinate("Corregidora", 20.5744, -100.4314)
        )),
        MexicanState("Quintana Roo", "QR", listOf(
            CityCoordinate("Cancún", 21.1619, -86.8515),
            CityCoordinate("Playa del Carmen", 20.6296, -87.0739),
            CityCoordinate("Chetumal", 18.5141, -88.2947),
            CityCoordinate("Tulum", 20.2114, -87.4654)
        )),
        MexicanState("San Luis Potosí", "SLP", listOf(
            CityCoordinate("San Luis Potosí", 22.1565, -100.9855),
            CityCoordinate("Soledad de Graciano Sánchez", 22.1833, -100.9333),
            CityCoordinate("Ciudad Valles", 21.9853, -99.0147),
            CityCoordinate("Matehuala", 23.6497, -100.6408)
        )),
        MexicanState("Sinaloa", "SIN", listOf(
            CityCoordinate("Culiacán", 24.8091, -107.3940),
            CityCoordinate("Mazatlán", 23.2494, -106.4111),
            CityCoordinate("Los Mochis", 25.7904, -108.9958),
            CityCoordinate("Guasave", 25.5642, -108.4708)
        )),
        MexicanState("Sonora", "SON", listOf(
            CityCoordinate("Hermosillo", 29.0729, -110.9559),
            CityCoordinate("Ciudad Obregón", 27.4869, -109.9408),
            CityCoordinate("Nogales", 31.3082, -110.9442),
            CityCoordinate("Guaymas", 28.3000, -111.3833),
            CityCoordinate("Navojoa", 27.0667, -109.4333)
        )),
        MexicanState("Tabasco", "TAB", listOf(
            CityCoordinate("Villahermosa", 17.9895, -92.9474),
            CityCoordinate("Cárdenas", 17.9969, -93.3764),
            CityCoordinate("Comalcalco", 18.2667, -93.2167)
        )),
        MexicanState("Tamaulipas", "TAMPS", listOf(
            CityCoordinate("Reynosa", 26.0810, -98.2892),
            CityCoordinate("Matamoros", 25.8797, -97.5042),
            CityCoordinate("Nuevo Laredo", 27.4806, -99.5075),
            CityCoordinate("Tampico", 22.2556, -97.8689),
            CityCoordinate("Victoria", 23.7361, -99.1431)
        )),
        MexicanState("Tlaxcala", "TLAX", listOf(
            CityCoordinate("Tlaxcala", 19.3182, -98.2375),
            CityCoordinate("Apizaco", 19.3169, -98.1389),
            CityCoordinate("Huamantla", 19.3136, -97.9236)
        )),
        MexicanState("Veracruz", "VER", listOf(
            CityCoordinate("Veracruz", 19.1738, -96.1342),
            CityCoordinate("Xalapa", 19.5372, -96.9264),
            CityCoordinate("Coatzacoalcos", 18.1453, -94.4294),
            CityCoordinate("Poza Rica", 20.5222, -97.4656),
            CityCoordinate("Córdoba", 18.8847, -96.9236)
        )),
        MexicanState("Yucatán", "YUC", listOf(
            CityCoordinate("Mérida", 20.9674, -89.6237),
            CityCoordinate("Valladolid", 20.6964, -88.2019),
            CityCoordinate("Tizimín", 21.1425, -88.1469),
            CityCoordinate("Progreso", 21.2828, -89.6639)
        )),
        MexicanState("Zacatecas", "ZAC", listOf(
            CityCoordinate("Zacatecas", 22.7709, -102.5833),
            CityCoordinate("Fresnillo", 23.1797, -102.8683),
            CityCoordinate("Guadalupe", 22.7472, -102.5114)
        ))
    )

    /** Lista simple de nombres de estados para dropdowns */
    val STATE_NAMES: List<String> = MEXICAN_STATES.map { it.name }

    /** Niveles de servicio nacional */
    val SERVICE_TIERS: List<ServiceTier> = listOf(
        ServiceTier(
            id = "LOCAL",
            label = "Envío Local Urbano",
            minDistanceKm = 0.0,
            maxDistanceKm = 25.0,
            ratePerKmMxn = 6.50,
            ratePerMinuteMxn = 1.20,
            baseFareMxn = 22.0,
            estimatedSpeedKmh = 21.0,
            description = "Dentro de la misma ciudad o zona metropolitana"
        ),
        ServiceTier(
            id = "INTERCITY",
            label = "Envío Intercity (Entre Ciudades)",
            minDistanceKm = 25.0,
            maxDistanceKm = 200.0,
            ratePerKmMxn = 4.50,
            ratePerMinuteMxn = 0.60,
            baseFareMxn = 35.0,
            estimatedSpeedKmh = 70.0,
            description = "Entre ciudades del mismo estado o estados cercanos"
        ),
        ServiceTier(
            id = "NATIONAL",
            label = "Envío Nacional (Larga Distancia)",
            minDistanceKm = 200.0,
            maxDistanceKm = 3000.0,
            ratePerKmMxn = 3.20,
            ratePerMinuteMxn = 0.35,
            baseFareMxn = 50.0,
            estimatedSpeedKmh = 85.0,
            description = "Cobertura nacional entre cualquier estado de la República Mexicana"
        )
    )

    /**
     * Determina el nivel de servicio basado en la distancia.
     */
    fun determineServiceTier(distanceKm: Double): ServiceTier {
        return SERVICE_TIERS.firstOrNull { distanceKm >= it.minDistanceKm && distanceKm < it.maxDistanceKm }
            ?: SERVICE_TIERS.last()
    }

    /**
     * Determina si un envío es intra-estatal o interestatal
     * basado en los estados de origen y destino.
     */
    fun isInterstate(originState: String, destState: String): Boolean {
        return !originState.equals(destState, ignoreCase = true) &&
                originState.isNotBlank() && destState.isNotBlank()
    }

    /**
     * Obtiene las coordenadas de una ciudad específica.
     */
    fun getCityCoordinates(stateName: String, cityName: String): CityCoordinate? {
        val state = MEXICAN_STATES.find { it.name.equals(stateName, ignoreCase = true) } ?: return null
        return state.cities.find { it.name.equals(cityName, ignoreCase = true) }
            ?: state.cities.firstOrNull()
    }

    /**
     * Obtiene la lista de ciudades para un estado dado.
     */
    fun getCitiesForState(stateName: String): List<String> {
        return MEXICAN_STATES.find { it.name.equals(stateName, ignoreCase = true) }?.cities?.map { it.name }
            ?: emptyList()
    }
}
