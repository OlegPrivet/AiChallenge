# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kotlin Multiplatform** project using **Compose Multiplatform** for UI, targeting Android, iOS, and Desktop (JVM) platforms. The project uses a shared UI module (`sharedUI`) that contains the common code, with platform-specific entry points in `androidApp`, `iosApp`, and `desktopApp`.

## Build Commands

### Android
- Run in Android Studio: Open project and use the Android run configuration
- Build APK: `./gradlew :androidApp:assembleDebug`
- Find APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop
- Run desktop app: `./gradlew :desktopApp:run`
- Run with hot reload: `./gradlew :desktopApp:hotRun --auto`

### iOS
- Open `iosApp/iosApp.xcodeproj` in Xcode and run
- Or use the Kotlin Multiplatform Mobile plugin in Android Studio

### Testing
- Run tests: `./gradlew test`
- Run tests for specific module: `./gradlew :sharedUI:test`

### General Gradle
- List all tasks: `./gradlew tasks --all`
- Clean build: `./gradlew clean`
- Build all: `./gradlew build`

## Architecture

### Module Structure

1. **sharedUI** - Multiplatform shared module containing:
   - Common UI code using Compose Multiplatform
   - Source sets: `commonMain`, `androidMain`, `jvmMain`, `iosMain`
   - Platform-agnostic business logic and UI components
   - Builds as an Android library and iOS framework (named `SharedUI`)

2. **androidApp** - Android application entry point
   - Depends on `:sharedUI` module
   - Minimal code, delegates to shared UI via `AppActivity.kt`

3. **desktopApp** - JVM desktop application entry point
   - Depends on `:sharedUI` module
   - Entry point: `main.kt` with `mainClass = "MainKt"`
   - Supports hot reload during development

4. **iosApp** - iOS application (Xcode project)
   - Links to the `SharedUI` framework built from the sharedUI module

### Key Technologies

- **Compose Multiplatform**: Declarative UI framework for all platforms
- **Decompose**: Component lifecycle and navigation
- **Ktor**: HTTP client for networking (platform-specific engines: OkHttp for Android/JVM, Darwin for iOS)
- **Koin**: Dependency injection
- **Room**: Local database with KSP code generation for all platforms
- **kotlinx.serialization**: JSON serialization
- **Kermit**: Logging across platforms
- **multiplatform-settings**: Shared key-value storage

### Source Organization

```
sharedUI/src/
├── commonMain/          # Shared code for all platforms
│   ├── kotlin/          # Kotlin source files
│   └── composeResources/ # Compose resources (drawables, fonts, strings)
├── androidMain/         # Android-specific implementations
├── jvmMain/             # Desktop-specific implementations
├── iosMain/             # iOS-specific implementations
└── commonTest/          # Shared test code
```

### Dependency Management

- Uses Gradle version catalogs: `gradle/libs.versions.toml`
- All dependency versions centralized in the catalog
- Reference dependencies with `libs.` prefix (e.g., `libs.koin.core`)

### Platform-Specific Targets

- **Android**: minSdk 23, compileSdk 36
- **iOS**: Supports x64, arm64, and simulatorArm64
- **Desktop**: Targets DMG (macOS), MSI (Windows), DEB (Linux)

## Development Notes

### Hot Reload
Desktop supports Compose hot reload via `./gradlew :desktopApp:hotRun --auto` for rapid UI iteration without full restarts.

### Room Database
- Schema files stored in `sharedUI/schemas/`
- KSP generates code for all platforms (Android, JVM, iOS variants)
- Platform-specific KSP configurations in `sharedUI/build.gradle.kts:98-106`

### iOS Framework
- The `sharedUI` module compiles to a framework named `SharedUI`
- Framework configuration in `sharedUI/build.gradle.kts:81-86`
- Used by the native iOS app via `iosApp/iosApp.xcodeproj`

### Gradle Configuration
- Build cache and configuration cache enabled for faster builds
- Parallel execution enabled
- JVM args: `-Xmx8G` for both Gradle and Kotlin daemon
