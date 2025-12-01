# AiChallenge

A **Kotlin Multiplatform** project using **Compose Multiplatform** for shared UI across Android, iOS, and Desktop (JVM) platforms.

## Overview

This project demonstrates a modern multiplatform architecture with:
- **Shared UI** using Compose Multiplatform
- **Platform-specific entry points** for Android, iOS, and Desktop
- Common business logic and UI components across all platforms

## Architecture

### Module Structure

- **sharedUI** - Multiplatform module containing common UI and business logic
  - Source sets: `commonMain`, `androidMain`, `jvmMain`, `iosMain`
  - Builds as Android library and iOS framework (`SharedUI`)
- **androidApp** - Android application entry point
- **desktopApp** - JVM desktop application entry point
- **iosApp** - iOS application (Xcode project)

### Key Technologies

- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - Declarative UI framework
- [Decompose](https://github.com/arkivanov/Decompose) - Component lifecycle and navigation
- [Ktor](https://ktor.io/) - HTTP client with platform-specific engines
- [Koin](https://insert-koin.io/) - Dependency injection
- [Room](https://developer.android.com/kotlin/multiplatform/room) - Local database with KSP
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - JSON serialization
- [Kermit](https://github.com/touchlab/Kermit) - Multiplatform logging
- [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) - Shared key-value storage

## Build & Run

### Android
**Run on device/emulator:**
- Open project in Android Studio and use the Android run configuration

**Build APK:**
```bash
./gradlew :androidApp:assembleDebug
```
Find APK at: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop
**Run application:**
```bash
./gradlew :desktopApp:run
```

**Run with hot reload:**
```bash
./gradlew :desktopApp:hotRun --auto
```

### iOS
**Run on device/simulator:**
- Open `iosApp/iosApp.xcodeproj` in Xcode and run
- Or use the [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

## Testing

**Run all tests:**
```bash
./gradlew test
```

**Run tests for specific module:**
```bash
./gradlew :sharedUI:test
```

## Development

### Dependency Management
Dependencies are managed using Gradle version catalogs in `gradle/libs.versions.toml`

### Hot Reload
Desktop supports Compose hot reload for rapid UI iteration without full restarts.

### Platform Targets
- **Android**: minSdk 23, compileSdk 36
- **iOS**: Supports x64, arm64, and simulatorArm64
- **Desktop**: Targets DMG (macOS), MSI (Windows), DEB (Linux)

## Useful Commands

```bash
# List all available tasks
./gradlew tasks --all

# Clean build
./gradlew clean

# Build all modules
./gradlew build
```

## License

[Add your license here]
