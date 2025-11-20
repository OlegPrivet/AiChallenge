# Repository Guidelines

## Project Structure & Module Organization
- Compose Multiplatform KMP: modules `sharedUI`, `androidApp`, `desktopApp`; `iosApp` contains Xcode starter.
- `sharedUI/src/commonMain` holds shared UI/state/DI/networking; platform specifics in `androidMain`, `jvmMain`, `iosMain`; tests in `commonTest`.
- Desktop entry point `desktopApp/src/main/kotlin/main.kt`; Android and iOS apps are thin wrappers around SharedUI.

## Build, Test, and Development Commands
- `./gradlew build` compiles all targets and runs checks; `./gradlew clean` resets outputs.
- Desktop: `./gradlew :desktopApp:run` or `./gradlew :desktopApp:hotRun --auto` for hot reload.
- Android: `./gradlew :androidApp:assembleDebug` creates `androidApp/build/outputs/apk/debug/androidApp-debug.apk`; run/deploy via Android Studio.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode or use the KMP plugin; ensure the `SharedUI` framework builds first.
- Explore tasks with `./gradlew tasks --all` when unsure.

## Coding Style & Naming Conventions
- Kotlin official style (`kotlin.code.style=official`); 4-space indent; auto-format via IDE before committing.
- Package root `org.oleg.ai.challenge`; PascalCase composables/classes, camelCase functions/properties, UPPER_SNAKE constants; keep resource names lowercase with dashes/underscores.
- One composable/component per file with previews beside it; keep navigation state in Decompose components and wire dependencies via Koin.
- Prefer immutable state + coroutines/StateFlow; avoid ad-hoc singletons.

## Testing Guidelines
- `./gradlew test` for full suite; `./gradlew :sharedUI:test` for quick iterations on shared logic.
- Place cross-platform tests in `sharedUI/src/commonTest` mirroring source packages; use Compose uiTest and coroutines test helpers already configured.
- Cover Ktor clients, Room DAOs, and component flows with deterministic fakes; write behavior-focused names like `LoginComponentTest_showsErrorOn401`.

## Commit & Pull Request Guidelines
- Commit titles short and imperative (e.g., `Add chat schema validation`); keep scope narrow.
- PRs should summarize user-visible changes, note main commands run, and link related issues/tasks.
- Include screenshots or short clips for UI changes; call out migrations or config steps for reviewers.
- Keep secrets out of VCS; set `OPENROUTER_API_KEY` via `local.properties` or env vars (read by `buildConfig` in `sharedUI`).

## Configuration Notes
- Android SDK paths live in `local.properties`; don't paste secrets into PR text.
- Desktop window defaults come from `desktopApp/src/main/kotlin/main.kt`; adjust sizes there if UX requirements change.
