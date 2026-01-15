# Turbix - Gemini Context

This file provides the necessary architectural and operational context for Gemini to assist in the development of the **Turbix** (formerly Sensify) Android application.

## 🚀 Project Overview

**Turbix** is a high-performance native Android utility designed for helicopter test pilots. It quantifies atmospheric turbulence by isolating it from the airframe's mechanical vibrations.

### Key Features
- **HeliVibe Engine**: Implements ISO 2631-1 adapted scaling to convert vibration (RMS of Linear Acceleration) into a 0-10 Turbulence Rating.
- **One-Button "TARE"**: Allows pilots to set a baseline vibration signature in laminar air.
- **Real-time Visualization**: High-contrast UI with dynamic color states (Green to Red) based on turbulence severity.
- **Sensor Integration**: Uses `Sensor.TYPE_LINEAR_ACCELERATION` at `SENSOR_DELAY_FASTEST` for high-precision measurement.

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Asynchronous Logic**: Kotlin Coroutines & Flow
- **Data Visualization**: MPAndroidChart (via `mpchart` domain)

---

## 🛠 Building and Running

### Prerequisites
- **JDK**: 17 (Required for Gradle 8.10+)
- **Android SDK**: API 33 (Target)
- **JAVA_HOME**: Must point to a valid JDK 17 installation.

### Key Commands
- **Assemble Debug APK**: `./gradlew assembleDebug`
- **Install on Device**: `./gradlew installDebug`
- **Run Tests**: `./gradlew test` (Unit tests) / `./gradlew connectedAndroidTest` (Instrumented tests)
- **Check Linting**: `./gradlew lint`

### Preview in IDE
The project is configured for **Project IDX** Android Previews.
- Command: `./gradlew assembleDebug --no-daemon -Pandroid.injected.invoked.from.ide=true`
- Environment variables and SDK paths are managed in `.idx/dev.nix` and `local.properties`.

---

## 🏗 Project Structure

- `app/src/main/java/io/sensify/sensor/`:
    - `domains/`:
        - `helivibe/`: Core logic for turbulence calculation (`HeliVibeEngine.kt`).
        - `sensors/`: Hardware sensor management and packet delivery.
        - `chart/`: Integration with MPAndroidChart.
    - `ui/`:
        - `pages/helivibe/`: The primary "One-Button" interface.
        - `navigation/`: NavHost configuration.
        - `resource/`: Theming and shared UI assets.

---

## 📏 Development Conventions

### Coding Style
- **MVVM**: Business logic stays in `Engine` or `ViewModel` classes. UI is purely declarative via Compose.
- **Null Safety**: Always handle nullable sensor data (e.g., `packet.values?`) safely.
- **State Management**: Use `StateFlow` in ViewModels to expose UI state.
- **Sensor Lifecycle**: Sensors must be attached in `init` or `onStart` and detached in `onCleared` or `onStop` to save battery.

### ISO 2631-1 Scaling (HeliVibe)
| Delta RMS ($m/s^2$) | Score | Color State |
| :--- | :--- | :--- |
| < 0.1 | 0 | Green |
| 0.1 - 0.3 | 1-2 | Green |
| 0.3 - 0.5 | 3-4 | Yellow |
| 0.5 - 0.8 | 5-6 | Amber |
| 0.8 - 1.6 | 7-8 | Orange |
| > 1.6 | 9-10 | Red |

---

## 📝 TODOs & Future Roadmap
- [ ] Implement CSV logging for flight data.
- [ ] Add sensitivity toggle in settings.
- [ ] Optimize RMS calculation with a sum-of-squares accumulator for very large buffers.
- [ ] Ensure `KEEP_SCREEN_ON` is robustly handled across all page transitions.
