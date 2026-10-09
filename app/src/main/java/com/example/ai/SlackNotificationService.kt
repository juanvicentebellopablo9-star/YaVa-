package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Slack Notification Service for YaVa! Operations Team Alerts.
 *
 * Sends real-time Slack messages via Incoming Webhook whenever an order
 * changes status, alerting the operations team so they can monitor
 * shipments nationwide.
 *
 * The webhook URL is injected at build time via the Secrets Gradle Plugin
 * (BuildConfig.SLACK_WEBHOOK_URL). If no valid webhook is configured,
 * notifications are silently skipped — the app continues to function normally.
 */
object SlackNotificationService {

    private const val TAG = "SlackNotificationService"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns true if a valid Slack webhook URL is configured.
     */
    fun isConfigured(): Boolean {
        val url = BuildConfig.SLACK_WEBHOOK_URL
        return url.isNotBlank() && url.startsWith("https://hooks.slack.com/")
    }

    /**
     * Sends a Slack notification about an order status change to the operations team channel.
     * Call from a coroutine — uses IO dispatcher internally.
     *
     * @param trackingCode  Order tracking code (e.g. YAVA-12345)
     * @param status        New status (e.g. "Creado", "Aceptado", "En camino", "Entregado")
     * @param clientName   Client name
     * @param originState   Origin state (e.g. "Ciudad de México")
     * @param destState     Destination state (e.g. "Jalisco")
     * @param priceMxn      Total price in MXN
     * @param serviceTier   Service tier (LOCAL, INTERCITY, NATIONAL)
     * @param extraInfo     Optional extra context (e.g. driver name, payment method)
     */
    suspend fun notifyOrderStatusChange(
        trackingCode: String,
        status: String,
        clientName: String,
        originState: String,
        destState: String,
        priceMxn: Double,
        serviceTier: String,
        extraInfo: String = ""
    ) {
        if (!isConfigured()) {
            Log.d(TAG, "Slack webhook not configured — skipping notification for $trackingCode")
            return
        }

        val emoji = when (status) {
            "Creado", "Esperando conductor" -> "🆕"
            "Aceptado" -> "✅"
            "En camino" -> "🛵"
            "Entregado" -> "🎉"
            "Cancelado" -> "❌"
            else -> "📦"
        }

        val route = if (originState.isNotBlank() && destState.isNotBlank()) {
            "$originState → $destState"
        } else {
            "Ruta no especificada"
        }

        val tierLabel = when (serviceTier) {
            "NATIONAL" -> "Nacional"
            "INTERCITY" -> "Intercity"
            else -> "Local"
        }

        val text = buildString {
            appendLine("$emoji *YaVa! — Actualización de Envío*")
            appendLine("*Código:* $trackingCode")
            appendLine("*Estado:* $status")
            appendLine("*Cliente:* $clientName")
            appendLine("*Ruta:* $route ($tierLabel)")
            appendLine("*Precio:* \$${String.format(java.util.Locale.US, "%.2f", priceMxn)} MXN")
            if (extraInfo.isNotBlank()) {
                appendLine("*Detalle:* $extraInfo")
            }
            appendLine("_Equipo de Operaciones — YaVa! Logistics_")
        }

        sendSlackMessage(text)
    }

    /**
     * Sends a raw text message to the configured Slack webhook.
     */
    private suspend fun sendSlackMessage(text: String) {
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().put("text", text).toString()
                val request = Request.Builder()
                    .url(BuildConfig.SLACK_WEBHOOK_URL)
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Slack notification sent successfully")
                    } else {
                        Log.w(TAG, "Slack notification failed: HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Slack notification error: ${e.message}")
            }
        }
    }
}
