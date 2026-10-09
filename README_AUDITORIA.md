# YaVa! Express — Código Fuente para Auditoría Técnica

> **Versión:** 1.0 (APK release de 23 MB — `app-release.apk`, 23,219,663 bytes)  
> **Fecha de compilación:** 9 de octubre de 2026  
> **Rama Git:** `national-platform-expansion` (commit 7a49973857)  
> **Application ID:** `com.aistudio.yava.logistics`  
> **Package namespace:** `com.example`  
> **compileSdk:** 36 (minorApiLevel 1) | **minSdk:** 24 | **targetSdk:** 36

---

## 1. Instrucciones de Compilación

### Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17+ |
| Gradle | 9.3.1 (incluido vía wrapper) |
| Android Gradle Plugin | 9.1.1 |
| Kotlin | 2.2.10 |
| KSP | 2.3.5 |
| Android Studio | Koala Feature Drop o superior (recomendado) |

### Pasos

```bash
# 1. Copiar el archivo de ejemplo de variables de entorno
cp .env.example .env

# 2. Editar .env con tus claves reales (ver sección 3)
#    - GEMINI_API_KEY
#    - MAPS_API_KEY
#    - SLACK_WEBHOOK_URL
#    - FIREBASE_PROJECT_ID
#    - FIREBASE_API_KEY
#    - FIREBASE_APP_ID
#    - STORE_PASSWORD / KEY_PASSWORD (solo para release)

# 3. Colocar google-services.json en app/
#    (Descargar desde Firebase Console > Project Settings > Your Apps)

# 4. Compilar debug APK
./gradlew assembleDebug

# 5. Compilar release APK (requiere keystore)
#    Generar keystore:
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias upload
#    Luego:
./gradlew assembleRelease
#    El APK firmado se genera en:
#    app/build/outputs/apk/release/app-release.apk
```

### Ejecutar pruebas

```bash
# Pruebas unitarias
./gradlew test

# Pruebas instrumentadas (requieren emulador/dispositivo)
./gradlew connectedAndroidTest

# Capturas de pantalla con Roborazzi
./gradlew verifyRoborazziDebug
```

> **Nota:** Robolectric tests deben usar `@Config(sdk = [34])` — SDK 36 no es soportado por Robolectric 4.16.1.

---

## 2. Dependencias Principales

| Categoría | Librería | Versión |
|---|---|---|
| UI | Jetpack Compose BOM | 2024.09.00 |
| UI | Material 3 | (via BOM) |
| UI | Navigation Compose | 2.8.9 |
| UI | Coil (imágenes) | 2.7.0 |
| Firebase | Firebase BOM | 34.15.0 |
| Firebase | Firestore | (via BOM) |
| Firebase | Auth | (via BOM) |
| Firebase | AI (Gemini) | (via BOM) |
| Firebase | App Check (reCAPTCHA) | (via BOM) |
| Auth | Credentials Manager | 1.5.0 |
| Auth | Google ID | 1.1.1 |
| Maps | Play Services Maps | 19.1.0 |
| Maps | Maps Compose | 6.4.0 |
| Maps | Play Services Location | 21.3.0 |
| DB | Room Runtime + KTX | 2.7.0 |
| Red | OkHttp | 4.10.0 |
| Red | Retrofit | 2.12.0 |
| Red | Moshi | 1.15.2 |
| Serialización | kotlinx.serialization JSON | 1.8.0 |
| Coroutines | Core + Android | 1.10.2 |
| QR | ZXing Core | 3.5.3 |
| Testing | JUnit 4 | 4.13.2 |
| Testing | Robolectric | 4.16.1 |
| Testing | Roborazzi | 1.59.0 |

### Plugins de Gradle

- `com.android.application` 9.1.1
- `org.jetbrains.kotlin.plugin.compose` 2.2.10
- `org.jetbrains.kotlin.plugin.serialization` 2.2.10
- `com.google.devtools.ksp` 2.3.5
- `com.google.android.secrets-gradle-plugin` 2.0.1
- `com.google.gms.google-services` 4.5.0
- `io.github.takahirom.roborazzi` 1.59.0

---

## 3. Variables de Entorno (.env.example)

El archivo `.env.example` incluye todas las variables necesarias. **No se incluyen valores reales.**

| Variable | Propósito | Obligatoria |
|---|---|---|
| `GEMINI_API_KEY` | Llamadas a Gemini AI REST API | Para IA funcional |
| `MAPS_API_KEY` | Google Maps SDK + Maps Compose | Para mapas funcionales |
| `SLACK_WEBHOOK_URL` | Alertas de operaciones vía Slack | Para notificaciones |
| `FIREBASE_PROJECT_ID` | ID del proyecto Firebase | Para Auth + Firestore |
| `FIREBASE_API_KEY` | Web API Key de Firebase | Para Auth + Firestore |
| `FIREBASE_APP_ID` | App ID de Firebase Android | Para Auth + Firestore |
| `STORE_PASSWORD` | Password del keystore release | Solo para release |
| `KEY_PASSWORD` | Password de la clave (opcional) | Solo para release |

### google-services.json

**No se incluye en este ZIP.** Descargar desde:
- Firebase Console → Project Settings → Your Apps → Android → `google-services.json`

> El plugin `google-services` usa `MissingGoogleServicesStrategy.WARN`, por lo que la compilación no falla sin el archivo, pero Auth y Firestore no funcionarán.

---

## 4. Estructura del Proyecto

```
├── app/
│   ├── build.gradle.kts                    # Configuración de build, signing, dependencias
│   ├── google-services.json                # NO INCLUIDO (descargar de Firebase Console)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/example/
│       │       ├── MainActivity.kt          # Entry point, navegación
│       │       ├── ai/
│       │       │   ├── AraSystemLucid.kt     # Sistema de prompts de IA
│       │       │   ├── FirestoreService.kt  # Firebase Firestore (real)
│       │       │   ├── GeminiAiClient.kt    # Gemini AI REST client (real)
│       │       │   ├── GeminiSystemPrompts.kt
│       │       │   └── SlackNotificationService.kt  # Slack webhooks (real)
│       │       ├── data/
│       │       │   ├── Daos.kt              # Room DAOs
│       │       │   ├── Entities.kt          # Entidades Room
│       │       │   ├── NationalCoverage.kt  # Cobertura nacional (estados/ciudades)
│       │       │   ├── PricingCalculator.kt # Tarifas dinámicas
│       │       │   ├── YaVaDatabase.kt      # Room database
│       │       │   └── YaVaRepository.kt    # Repositorio (Room + Firestore)
│       │       └── ui/
│       │           ├── components/
│       │           │   ├── DeliveryEvidenceDialog.kt
│       │           │   ├── DirectorLoginDialog.kt
│       │           │   ├── GpsLocationHelper.kt       # GPS real (FusedLocationProvider)
│       │           │   ├── LegalModule.kt
│       │           │   ├── NotificationServiceHelper.kt
│       │           │   ├── QrCodeHelper.kt           # Generación QR (ZXing)
│       │           │   ├── RealtimeDeliveryProgressTracker.kt
│       │           │   ├── ReceiptPdfGenerator.kt
│       │           │   ├── TermsAndConditionsPdfGenerator.kt
│       │           │   ├── VoiceAssistantHelper.kt
│       │           │   ├── WhatsAppButton.kt
│       │           │   ├── YaVaContactCard.kt
│       │           │   ├── YaVaGoogleMapsTracker.kt   # Google Maps Compose (real)
│       │           │   ├── YaVaInteractiveMap.kt      # Mapa WebView interactivo
│       │           │   └── YaVaStatusTimeline.kt
│       │           ├── screens/
│       │           │   ├── AdminPortalScreen.kt
│       │           │   ├── AuthGatewayScreen.kt      # Google Sign-In (Credential Manager)
│       │           │   ├── CustomerRequestScreen.kt
│       │           │   ├── DriverPortalScreen.kt
│       │           │   ├── GeminiAiAssistantScreen.kt
│       │           │   ├── HomeScreen.kt
│       │           │   ├── MandatoryTermsGateScreen.kt
│       │           │   └── TrackingMapScreen.kt
│       │           ├── theme/
│       │           │   ├── Color.kt
│       │           │   ├── Theme.kt
│       │           │   └── Type.kt
│       │           └── viewmodel/
│       │               └── YaVaViewModel.kt          # ViewModel central
│       ├── test/                                  # Pruebas unitarias
│       └── androidTest/                           # Pruebas instrumentadas
├── gradle/
│   ├── libs.versions.toml                         # Catálogo de versiones
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                               # Build raíz
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── .env.example                                    # Variables de entorno (sin valores reales)
├── .gitignore
└── README_AUDITORIA.md                             # Este archivo
```

---

## 5. Auditoría: Funciones Reales vs. Simulaciones

### ✅ Funciones con Servicios Reales

| Módulo | Archivo | Servicio Real | Notas |
|---|---|---|---|
| **Firebase Auth (Google Sign-In)** | `AuthGatewayScreen.kt` | Credential Manager + Firebase Auth | Usa `Web Client ID` de Google Cloud. Requiere `google-services.json` válido y SHA-1 del keystore registrado en Firebase. |
| **Firebase Firestore** | `FirestoreService.kt` | Cloud Firestore | Sincronización en tiempo real de órdenes y estado de entrega. Inicializa Firebase con `BuildConfig` (project ID, API key, app ID). |
| **Gemini AI** | `GeminiAiClient.kt` | Gemini REST API (`generativelanguage.googleapis.com`) | Llamadas REST directas con Retrofit. Modelos: `gemini-3.5-flash`, `gemini-3.1-pro-preview`. Soporta Google Search grounding y Google Maps tools. |
| **Google Maps** | `YaVaGoogleMapsTracker.kt` | Google Maps Compose SDK | Renderiza mapa nativo, marcadores, polylines, cámara, tráfico. Requiere `MAPS_API_KEY` válida. |
| **GPS / Ubicación** | `GpsLocationHelper.kt` | FusedLocationProviderClient | GPS real del dispositivo. Solicita permisos `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`. |
| **Slack Notificaciones** | `SlackNotificationService.kt` | Slack Incoming Webhooks | POST HTTP a webhook URL. Alerta cambios de estado de orden al equipo de operaciones. |
| **Base de datos local** | `YaVaDatabase.kt`, `Daos.kt`, `Entities.kt` | Room (SQLite) | Persistencia local de órdenes, conductores, tarifas. |
| **Generación QR** | `QrCodeHelper.kt` | ZXing Core | Genera códigos QR reales para identificación de órdenes. |
| **Generación PDF** | `ReceiptPdfGenerator.kt`, `TermsAndConditionsPdfGenerator.kt` | Android Canvas/PDF API | Genera PDFs nativos de recibos y términos. |
| **Notificaciones** | `NotificationServiceHelper.kt` | Android NotificationManager | Notificaciones push locales del sistema Android. |

### ⚠️ Funciones con Degradación / Modo Fallback

| Módulo | Archivo | Comportamiento sin API Key |
|---|---|---|
| **Gemini AI** | `GeminiAiClient.kt` | Si `GEMINI_API_KEY` está vacía o es `MY_GEMINI_API_KEY`, responde con mensaje local predefinido. No hay llamada al servidor. |
| **Gemini Transcripción** | `GeminiAiClient.kt` | Sin API key, devuelve texto simulado: *"Quiero un envío desde el centro hasta Polanco CDMX"*. |
| **Google Maps** | `YaVaGoogleMapsTracker.kt` | Sin `MAPS_API_KEY`, el mapa no renderiza tiles. Los marcadores y polylines se calculan pero no se muestran sobre el mapa base. |
| **Mapa WebView** | `YaVaInteractiveMap.kt` | Usa WebView con OpenStreetMap tiles como fallback cuando Maps SDK no tiene API key. |

### 🔧 Funciones de Lógica Interna (no dependen de servicios externos)

| Módulo | Archivo | Descripción |
|---|---|---|
| **Tarifas Dinámicas** | `PricingCalculator.kt` | Cálculo de tarifas basado en distancia, tipo de servicio, factor de demanda y cobertura nacional. 100% lógica local. |
| **Cobertura Nacional** | `NationalCoverage.kt` | Catálogo de estados y ciudades de México con coordenadas. Datos embebidos. |
| **Asignación de Repartidores** | `YaVaViewModel.kt` | Lógica de asignación basada en proximidad y disponibilidad. Usa Room local. |
| **Rastreo de Progreso** | `RealtimeDeliveryProgressTracker.kt` | Animación de progreso de entrega. Simula movimiento del repartidor entre origen y destino. |
| **Sistema de Prompts IA** | `AraSystemLucid.kt`, `GeminiSystemPrompts.kt` | Prompts del sistema para Gemini. Define personalidad y capacidades del asistente. |
| **Voice Assistant** | `VoiceAssistantHelper.kt` | Helper para entrada de voz. Usa Android SpeechRecognizer. |
| **WhatsApp** | `WhatsAppButton.kt` | Intent de WhatsApp. Abre la app si está instalada. |
| **Login Director** | `DirectorLoginDialog.kt` | Autenticación local de administrador (no Firebase). |
| **Términos y Condiciones** | `MandatoryTermsGateScreen.kt`, `LegalModule.kt` | Flujo de aceptación de términos legal. |

---

## 6. Seguridad — Archivos Excluidos

Los siguientes archivos **NO se incluyen** en este ZIP:

| Archivo | Motivo |
|---|---|
| `my-upload-key.jks` | Keystore de firma release (clave privada) |
| `debug.keystore` | Keystore de debug |
| `.keystore-creds` | Credenciales del keystore |
| `.env` | Variables de entorno con valores reales |
| `app/google-services.json` | Configuración Firebase con API keys y OAuth client IDs |
| `/run/base44/app.env` | Secrets del platform (fuera del repo) |

**Solo se incluye `.env.example` con valores placeholder.**

---

## 7. Configuración Firebase

El proyecto Firebase es **yava--mexico**:
- **Project ID:** `yava--mexico`
- **App ID:** `1:1024794932827:android:fb034add69be770e52f17e`
- **OAuth Web Client ID:** `1024794932827-2psu0q91ijf9ie35h3och3kfic51qbqk.apps.googleusercontent.com`
- **SHA-1 del keystore release:** `88:6E:29:55:94:FE:40:B1:27:F8:FE:86:FB:2D:B1:DA:BB:66:33:31`

> Estos valores están en `.env.example` y `AuthGatewayScreen.kt` como referencias.  
> Para reproducción: descargar `google-services.json` desde Firebase Console y colocar en `app/`.

---

## 8. Configuración de Firma Release

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("STORE_PASSWORD")  // PKCS12 usa un solo password
    }
}
```

Para reproducir la firma:
1. Generar un nuevo keystore: `keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. Configurar `STORE_PASSWORD` en `.env`
3. Registrar el SHA-1 del nuevo keystore en Firebase Console > Project Settings > Your Apps

---

## 9. Notas para el Auditor

- **No se modificó, recompiló ni generó nueva versión** para este paquete.
- El código corresponde exactamente al commit `7a49973857` de la rama `national-platform-expansion`.
- El APK release de 23 MB fue compilado con este código fuente exacto.
- Las funciones marcadas como "reales" requieren credenciales válidas para funcionar; sin ellas, degradan a modo fallback o simulación.
- La lógica de tarifas dinámicas, cobertura nacional y asignación de repartidores es 100% local y no depende de servicios externos.
- El repositorio usa el Secrets Gradle Plugin para inyectar API keys desde `.env` en `BuildConfig`.
- Firebase Firestore se inicializa programáticamente en `FirestoreService.kt` con fallback sandbox.
- La app usa doble motor de mapas: Leaflet OSM (WebView, sin API key) y Canvas vector HUD como fallback.
- Coordenadas nacionales por defecto: CDMX (19.4326, -99.1332).

---

*Documento generado para auditoría técnica externa — YaVa! Express v1.0*
