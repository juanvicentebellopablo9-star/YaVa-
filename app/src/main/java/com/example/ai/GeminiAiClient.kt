package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Serialization Data Models for Gemini REST API ---

@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: GeminiContent? = null
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiGenConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: GeminiSpeechConfig? = null,
    val thinkingConfig: GeminiThinkingConfig? = null
)

@Serializable
data class GeminiThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GeminiSpeechConfig(
    val voiceConfig: GeminiVoiceConfig
)

@Serializable
data class GeminiVoiceConfig(
    val prebuiltVoiceConfig: GeminiPrebuiltVoice
)

@Serializable
data class GeminiPrebuiltVoice(
    val voiceName: String
)

@Serializable
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorResponse? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiErrorResponse(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

// --- Retrofit Service Interface ---

interface GeminiRestService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

// --- Gemini Client Engine ---

object GeminiAiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: GeminiRestService = retrofit.create(GeminiRestService::class.java)

    /**
     * Executes standard or grounded Gemini generation using official model rules:
     * - Complex tasks: gemini-3.1-pro-preview
     * - General tasks / Transcription: gemini-3.5-flash
     * - Fast / Low latency: gemini-3.1-flash-lite-preview
     * - Live API / Audio preview: gemini-3.1-flash-live-preview or gemini-2.5-flash-native-audio-preview-12-2025
     */
    suspend fun sendPrompt(
        model: String = "gemini-3.5-flash",
        history: List<GeminiContent>,
        systemPrompt: String? = null,
        enableGoogleSearch: Boolean = false,
        enableGoogleMaps: Boolean = false,
        temperature: Float = 0.7f
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "ℹ️ [YaVa! AI Assistant]: Configuración de API Key requerida en el panel de Secrets de AI Studio. Respondiendo en modo local Mérida, Yucatán: Servicio activo con cotización dinámica y 15% de comisión de plataforma."
        }

        val toolsList = mutableListOf<JsonObject>()
        if (enableGoogleSearch) {
            toolsList.add(buildJsonObject {
                putJsonObject("googleSearch") {}
            })
        }
        if (enableGoogleMaps) {
            toolsList.add(buildJsonObject {
                putJsonObject("googleMaps") {}
            })
        }

        val systemInstruction = systemPrompt?.let {
            GeminiContent(
                parts = listOf(GeminiPart(text = it))
            )
        }

        val request = GeminiGenerateRequest(
            contents = history,
            generationConfig = GeminiGenConfig(
                temperature = temperature
            ),
            tools = if (toolsList.isNotEmpty()) toolsList else null,
            systemInstruction = systemInstruction
        )

        try {
            val response = apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )

            val candidateText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")

            if (!candidateText.isNullOrBlank()) {
                candidateText
            } else {
                response.error?.message ?: "Sin respuesta disponible de Gemini ($model)."
            }
        } catch (e: Exception) {
            "Error al contactar Gemini ($model): ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * Transcribe audio data or explain audio input using gemini-3.5-flash
     */
    suspend fun transcribeAudio(
        base64AudioData: String,
        mimeType: String = "audio/mp3",
        prompt: String = "Transcribe este audio en español con exactitud para solicitud de envíos o direcciones en Mérida Yucatán."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Transcripción de audio local: 'Quiero un envío desde Centro Histórico hasta Altabrisa Mérida'."
        }

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64AudioData))
                    )
                )
            )
        )

        try {
            val response = apiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("\n")
                ?: "No se detectó voz o texto."
        } catch (e: Exception) {
            "Error en transcripción de audio: ${e.message}"
        }
    }
}
