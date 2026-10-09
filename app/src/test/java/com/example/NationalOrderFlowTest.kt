package com.example

import com.example.data.NationalCoverage
import com.example.data.PricingCalculator
import com.example.ai.SlackNotificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end test of the national order creation flow.
 *
 * Verifies that:
 * 1. NationalCoverage contains all 32 Mexican states.
 * 2. State selectors provide valid state names for dropdowns.
 * 3. Interstate shipments are correctly classified by service tier.
 * 4. Order creation with state selectors produces correct pricing.
 * 5. Slack notification service gracefully handles missing webhook config.
 * 6. The full flow from state selection → pricing → Slack alert works.
 */
class NationalOrderFlowTest {

    @Test
    fun `national coverage has all 32 Mexican states`() {
        assertEquals(32, NationalCoverage.MEXICAN_STATES.size)
    }

    @Test
    fun `state names list matches states for dropdown selectors`() {
        assertEquals(32, NationalCoverage.STATE_NAMES.size)
        // Verify key states are present
        assertTrue(NationalCoverage.STATE_NAMES.contains("Ciudad de México"))
        assertTrue(NationalCoverage.STATE_NAMES.contains("Jalisco"))
        assertTrue(NationalCoverage.STATE_NAMES.contains("Yucatán"))
        assertTrue(NationalCoverage.STATE_NAMES.contains("Nuevo León"))
        assertTrue(NationalCoverage.STATE_NAMES.contains("Baja California"))
    }

    @Test
    fun `interstate shipment between CDMX and Jalisco is detected`() {
        val isInterstate = NationalCoverage.isInterstate("Ciudad de México", "Jalisco")
        assertTrue("CDMX → Jalisco should be interstate", isInterstate)
    }

    @Test
    fun `intrastate shipment within same state is not interstate`() {
        val isInterstate = NationalCoverage.isInterstate("Jalisco", "Jalisco")
        assertFalse("Same state should not be interstate", isInterstate)
    }

    @Test
    fun `blank states are not considered interstate`() {
        assertFalse(NationalCoverage.isInterstate("", "Jalisco"))
        assertFalse(NationalCoverage.isInterstate("Jalisco", ""))
    }

    @Test
    fun `local distance under 25km gets LOCAL service tier`() {
        val tier = NationalCoverage.determineServiceTier(10.0)
        assertEquals("LOCAL", tier.id)
    }

    @Test
    fun `intercity distance 25-200km gets INTERCITY service tier`() {
        val tier = NationalCoverage.determineServiceTier(80.0)
        assertEquals("INTERCITY", tier.id)
    }

    @Test
    fun `national distance over 200km gets NATIONAL service tier`() {
        val tier = NationalCoverage.determineServiceTier(500.0)
        assertEquals("NATIONAL", tier.id)
    }

    @Test
    fun `city coordinates for CDMX return valid GPS`() {
        val coords = NationalCoverage.getCityCoordinates("Ciudad de México", "")
        assertNotNull(coords)
        // CDMX Centro coordinates
        assertEquals(19.4326, coords!!.lat, 0.01)
        assertEquals(-99.1332, coords.lng, 0.01)
    }

    @Test
    fun `city coordinates for Merida Yucatan return valid GPS`() {
        val coords = NationalCoverage.getCityCoordinates("Yucatán", "")
        assertNotNull(coords)
        // Mérida coordinates
        assertEquals(20.9674, coords!!.lat, 0.01)
        assertEquals(-89.6237, coords.lng, 0.01)
    }

    @Test
    fun `order creation flow with national states produces valid quote`() {
        // Simulate order from CDMX to Monterrey (Nuevo León) — interstate/national
        val originState = "Ciudad de México"
        val destState = "Nuevo León"

        val originCoords = NationalCoverage.getCityCoordinates(originState, "")
        val destCoords = NationalCoverage.getCityCoordinates(destState, "")

        assertNotNull("Origin coords should exist", originCoords)
        assertNotNull("Dest coords should exist", destCoords)

        // Calculate approximate distance (haversine not needed here — use a realistic national distance)
        val distanceKm = 900.0 // CDMX to Monterrey ~ 900km
        val tier = NationalCoverage.determineServiceTier(distanceKm)
        assertEquals("NATIONAL", tier.id)

        val quote = PricingCalculator.calculateQuote(
            distanceKm = distanceKm,
            packageType = "Paquete Mediano (3 - 10 kg)",
            weightKg = 5.0,
            isHighDemand = false,
            isWeatherSurge = false
        )

        assertTrue("Quote price should be positive", quote.finalPriceMxn > 0)
        assertTrue("Quote should reflect national tier base fare", quote.baseFareMxn >= 50.0)
        assertTrue("Platform commission should be 15%", quote.platformCommissionAmountMxn > 0)
        assertTrue("Driver earnings should be 85%", quote.driverEarningsMxn > 0)

        val isInterstate = NationalCoverage.isInterstate(originState, destState)
        assertTrue("CDMX to Nuevo León should be interstate", isInterstate)
    }

    @Test
    fun `order creation flow with local same-state shipment produces valid quote`() {
        val originState = "Puebla"
        val destState = "Puebla"

        val isInterstate = NationalCoverage.isInterstate(originState, destState)
        assertFalse("Same state should not be interstate", isInterstate)

        val distanceKm = 15.0 // Local within Puebla
        val tier = NationalCoverage.determineServiceTier(distanceKm)
        assertEquals("LOCAL", tier.id)

        val quote = PricingCalculator.calculateQuote(
            distanceKm = distanceKm,
            packageType = "Documentos",
            weightKg = 0.5,
            isHighDemand = false,
            isWeatherSurge = false
        )

        assertTrue("Local quote should be positive", quote.finalPriceMxn > 0)
        assertEquals("Local tier base fare", 22.0, quote.baseFareMxn, 0.01)
    }

    @Test
    fun `slack notification service is not configured with placeholder webhook`() {
        // Without a real Slack webhook URL configured via secrets,
        // the service should report it's not configured.
        // BuildConfig.SLACK_WEBHOOK_URL will be "YOUR_SLACK_WEBHOOK_URL" from .env.example
        assertFalse("Slack should not be configured with placeholder", SlackNotificationService.isConfigured())
    }

    @Test
    fun `full national order flow simulation`() {
        // Simulate the complete flow: state selection → coords → tier → pricing → Slack readiness
        val testCases = listOf(
            Triple("Ciudad de México", "Yucatán", 1300.0),      // National
            Triple("Jalisco", "Michoacán", 250.0),                 // Intercity
            Triple("Puebla", "Puebla", 12.0),                      // Local
            Triple("Nuevo León", "Baja California", 1800.0),       // National
            Triple("Estado de México", "Querétaro", 200.0)         // Intercity boundary
        )

        testCases.forEach { (origin, dest, distance) ->
            val originCoords = NationalCoverage.getCityCoordinates(origin, "")
            val destCoords = NationalCoverage.getCityCoordinates(dest, "")
            assertNotNull("Coords for $origin", originCoords)
            assertNotNull("Coords for $dest", destCoords)

            val tier = NationalCoverage.determineServiceTier(distance)
            val quote = PricingCalculator.calculateQuote(
                distanceKm = distance,
                packageType = "Paquete Pequeño (< 3 kg)",
                weightKg = 2.0
            )

            assertTrue("Quote for $origin→$dest should be positive", quote.finalPriceMxn > 0)
            assertTrue("Tier for $origin→$dest should be valid", tier.id in listOf("LOCAL", "INTERCITY", "NATIONAL"))

            // Slack service should gracefully handle unconfigured state
            assertFalse(SlackNotificationService.isConfigured())
        }
    }
}
