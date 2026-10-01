package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.FirestoreService
import com.example.ai.GeminiAiClient
import com.example.ai.GeminiContent
import com.example.ai.GeminiPart
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.YaVaViewModel
import kotlinx.coroutines.launch
import java.util.Locale

enum class GeminiFeatureMode(
    val displayName: String,
    val modelId: String,
    val icon: ImageVector,
    val description: String
) {
    GENERAL_CHAT(
        "Chatbot Asistente",
        "gemini-3.5-flash",
        Icons.Default.Chat,
        "Asistente general de YaVa! Logistics (gemini-3.5-flash)"
    ),
    MAPS_GROUNDING(
        "Google Maps Grounding",
        "gemini-3.5-flash",
        Icons.Default.Map,
        "Validación de ubicaciones y negocios en Mérida (Google Maps Tool)"
    ),
    SEARCH_GROUNDING(
        "Google Search Grounding",
        "gemini-3.5-flash",
        Icons.Default.Search,
        "Búsqueda web en vivo de tráfico y noticias (Google Search Tool)"
    ),
    COMPLEX_REASONING(
        "Gemini Intelligence",
        "gemini-3.1-pro-preview",
        Icons.Default.Psychology,
        "Optimización de rutas complejas y análisis contable (gemini-3.1-pro-preview)"
    ),
    LOW_LATENCY(
        "Respuestas Rápidas",
        "gemini-3.1-flash-lite-preview",
        Icons.Default.Bolt,
        "Consultas ultrarrápidas con baja latencia (gemini-3.1-flash-lite-preview)"
    ),
    VOICE_LIVE(
        "Voz en Vivo",
        "gemini-3.1-flash-live-preview",
        Icons.Default.GraphicEq,
        "Conversaciones por voz en tiempo real (gemini-3.1-flash-live-preview)"
    )
}

data class ChatBubble(
    val id: String,
    val sender: String, // "user" or "gemini"
    val text: String,
    val modelTag: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAiAssistantScreen(
    viewModel: YaVaViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val userEmail by viewModel.authenticatedUserEmail.collectAsState()
    val userName by viewModel.authenticatedUserName.collectAsState()

    var selectedMode by remember { mutableStateOf(GeminiFeatureMode.GENERAL_CHAT) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatBubble(
                id = "1",
                sender = "gemini",
                text = "¡Hola $userName! Soy tu asistente de inteligencia artificial YaVa! Logistics en Mérida, Yucatán. Puedo asistirte con cotizaciones dinámicas transparentes (15% comisión de plataforma), búsqueda con mapas, optimización de rutas y transcripción de voz.",
                modelTag = "gemini-2.5-flash",
                timestamp = "Ahora"
            )
        )
    }

    val listState = rememberLazyListState()

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            inputText = spokenText
        }
    }

    fun triggerSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla tu dirección o consulta para YaVa! Mérida...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // Speech not available
        }
    }

    fun sendMessage() {
        val textToSend = inputText.trim()
        if (textToSend.isBlank() || isLoading) return

        inputText = ""
        val userBubble = ChatBubble(
            id = System.currentTimeMillis().toString(),
            sender = "user",
            text = textToSend,
            modelTag = "Tú",
            timestamp = "Ahora"
        )
        messages.add(userBubble)

        isLoading = true

        coroutineScope.launch {
            // Build conversation history
            val geminiContents = messages.map { bubble ->
                GeminiContent(
                    role = if (bubble.sender == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = bubble.text))
                )
            }

            val systemInstruction = """
                Eres el asistente oficial de YaVa! Logistics, la empresa de envíos y paquetería local en Mérida, Yucatán, México.
                - Sistema de Tarifa Dinámica Modular YaVa!: Base ($22 MXN) + Distancia ($6.50/km) + Tiempo Tráfico ($1.20/min) + Recargos de peso/clima. Tarifa mínima: $35 MXN.
                - Comisión de plataforma: 15% para YaVa!, 85% ganancia neta para el socio repartidor.
                - Base de operaciones: Mérida, Yucatán (Altabrisa, Centro, Montejo, Francisco de Montejo, Ciudad Caucel, Las Américas, Kanasín, etc.).
                - Modalidad de pagos: Efectivo, Terminal con tarjeta, Transferencia SPEI (CLABE MercadoPago 722969010374423450 a nombre del Director Vicente Bello).
                - Sé servicial, rápido, cortés y muy exacto con direcciones yucatecas.
            """.trimIndent()

            val isMaps = selectedMode == GeminiFeatureMode.MAPS_GROUNDING
            val isSearch = selectedMode == GeminiFeatureMode.SEARCH_GROUNDING

            val response = GeminiAiClient.sendPrompt(
                model = selectedMode.modelId,
                history = geminiContents,
                systemPrompt = systemInstruction,
                enableGoogleMaps = isMaps,
                enableGoogleSearch = isSearch
            )

            val botBubble = ChatBubble(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = "gemini",
                text = response,
                modelTag = selectedMode.modelId,
                timestamp = "Ahora"
            )
            messages.add(botBubble)
            isLoading = false

            // Save to Firestore cloud
            FirestoreService.saveChatMessage(
                userId = userEmail.ifBlank { "anonymous" },
                chatMessage = com.example.ai.CloudChatMessage(
                    id = botBubble.id,
                    sender = "gemini",
                    message = response,
                    modelUsed = selectedMode.modelId
                )
            )

            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(YaVaYellowPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YaVa! Gemini Intelligence",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mérida Grounding • Voz • Modelos 2026",
                                fontSize = 10.sp,
                                color = YaVaYellowPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = selectedMode.modelId,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feature Mode Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GeminiFeatureMode.entries.take(3).forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YaVaYellowPrimary,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GeminiFeatureMode.entries.drop(3).forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.displayName, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YaVaYellowPrimary,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Active Mode Banner
        Surface(
            color = YaVaYellowPrimary.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = selectedMode.icon,
                    contentDescription = null,
                    tint = YaVaYellowPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedMode.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Message Thread List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { bubble ->
                val isUser = bubble.sender == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = bubble.modelTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.primary else YaVaYellowPrimary
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) YaVaYellowPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(
                            text = bubble.text,
                            fontSize = 13.sp,
                            color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = YaVaYellowPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini está analizando tu solicitud en Mérida...",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Input & Voice Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Voice Transcription Trigger
                IconButton(
                    onClick = { triggerSpeechInput() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(YaVaYellowPrimary.copy(alpha = 0.2f))
                        .testTag("btn_gemini_voice_mic")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictar por voz",
                        tint = YaVaYellowPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe una consulta o dirección...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_gemini_chat_text")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage() },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isLoading) YaVaYellowPrimary else Color.Gray.copy(alpha = 0.3f))
                        .testTag("btn_send_gemini_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (inputText.isNotBlank() && !isLoading) Color.Black else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
