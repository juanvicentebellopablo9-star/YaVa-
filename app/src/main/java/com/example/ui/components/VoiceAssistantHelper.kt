package com.example.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class VoiceAssistantHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val spanishLocale = Locale("es", "MX")
            val result = tts?.setLanguage(spanishLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es"))
            }

            // Configure warm, natural female pitch & cadence for Spanish
            tts?.setPitch(1.1f)
            tts?.setSpeechRate(0.95f)

            // Try selecting a Spanish female voice if available on device
            try {
                val voices = tts?.voices
                if (voices != null) {
                    val femaleVoice = voices.firstOrNull { voice ->
                        voice.locale.language == "es" && (voice.name.lowercase().contains("female") || voice.name.lowercase().contains("es-es-x") || voice.name.lowercase().contains("es-mx-x") || voice.name.lowercase().contains("f00") || voice.name.lowercase().contains("network"))
                    } ?: voices.firstOrNull { it.locale.language == "es" }
                    if (femaleVoice != null) {
                        tts?.voice = femaleVoice
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceAssistant", "Error setting voice pitch/female sound: ${e.message}")
            }

            isInitialized = true
        }
    }

    fun speak(message: String) {
        if (isInitialized) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "yava_voice_assistant_id")
        }
    }

    fun speakServiceAccepted(trackingCode: String, origin: String) {
        speak("¡Hola socio! Has aceptado el envío $trackingCode. Te guiaré paso a paso. Tu primera parada es el punto de recolección en: $origin. Maneja con prudencia.")
    }

    fun speakNavigatingToPickup(origin: String) {
        speak("Dirigiéndote hacia la recolección en $origin. Al llegar, solicita la firma o comprobante de entrega.")
    }

    fun speakPackageCollected(destination: String) {
        speak("¡Recolección confirmada con éxito! Ahora te dirijo al destino final en: $destination. Inicia la ruta en Google Maps cuando estés listo.")
    }

    fun speakDeliveryCompleted(trackingCode: String, priceMxn: Double) {
        speak("¡Felicidades socio! El pedido $trackingCode ha sido completado y entregado satisfactoriamente. Tu comisión por este viaje es de $$priceMxn pesos. ¡Excelente trabajo!")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
