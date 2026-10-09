# YaVa! Logistics — Android (Kotlin / Jetpack Compose)

## Project Overview
YaVa! is a national logistics and delivery platform for the Mexican market. Built with Kotlin, Jetpack Compose, Room, Firebase (Firestore + Auth), and Gemini AI.

## Build & Test

### Compile the debug APK
```bash
docker compose -f docker-compose.base44.yml run --rm --no-deps builder sh -c '
  export GRADLE_USER_HOME=/home/gradle/.gradle
  export ANDROID_HOME=/opt/android-sdk
  echo "sdk.dir=/opt/android-sdk" > /app/local.properties
  cd /app && chmod +x gradlew && ./gradlew assembleDebug --no-daemon
'
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Run unit tests
```bash
docker compose -f docker-compose.base44.yml run --rm --no-deps builder sh -c '
  export GRADLE_USER_HOME=/home/gradle/.gradle
  export ANDROID_HOME=/opt/android-sdk
  echo "sdk.dir=/opt/android-sdk" > /app/local.properties
  cd /app && chmod +x gradlew && ./gradlew testDebugUnitTest --no-daemon
'
```

## Key Technical Details

- **AGP 9.1.1** requires **Gradle 9.3.1+** and **JDK 17+**.
- **compileSdk 36** (Android 16), **minSdk 24**.
- Robolectric tests must use `@Config(sdk = [34])` — SDK 36 is not yet supported by Robolectric 4.16.1.
- The Gradle wrapper (`gradlew`, `gradle/wrapper/`) is not committed; it is generated inside the Docker container.
- `debug.keystore` is generated at build time (gitignored).
- `local.properties` with `sdk.dir` is generated at build time (gitignored).
- The Secrets Gradle Plugin reads `.env` and `.env.example` for `GEMINI_API_KEY` and `MAPS_API_KEY`.
- `google-services.json` is optional (plugin uses `MissingGoogleServicesStrategy.WARN`).
- Firebase Firestore is initialized programmatically in `FirestoreService.kt` with a fallback sandbox config.
- The app uses a dual-map engine: Leaflet OSM (WebView, zero API key) and a Canvas-based vector HUD fallback.
- National default coordinates are CDMX (19.4326, -99.1332) — all fallback GPS coordinates use CDMX, not Mérida.

## Architecture

- `data/` — Room entities, DAOs, database, repository, pricing calculator, national coverage catalog (32 states).
- `ai/` — Gemini AI client (REST API), Firestore service, ARA System Lucid analytics engine.
- `ui/screens/` — Compose screens: Home, CustomerRequest, DriverPortal, TrackingMap, AdminPortal, AuthGateway, GeminiAiAssistant, MandatoryTermsGate.
- `ui/components/` — Reusable Compose components: maps, GPS helper, QR, PDF generators, legal modules, voice assistant, contact cards.
- `ui/viewmodel/` — Single `YaVaViewModel` managing all state and business logic.
