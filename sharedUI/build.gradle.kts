
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

kotlin {
    android {
        namespace = "org.oleg.ai.challenge"
        compileSdk = 36
        minSdk = 23
        androidResources.enable = true
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            api(libs.decompose)
            api(libs.decompose.compose)
            api(libs.essenty.coroutines)
            implementation(libs.kotlinx.serialization.json)
            api(libs.koin.core)
            api(libs.koin.compose)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.icons)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries { framework { baseName = "SharedUI" } }
        }
}

buildConfig {
    // Generate BuildConfig class with API configuration
    packageName = "org.oleg.ai.challenge"

    // Read API key from local.properties or environment variable
    // To set the API key, add to local.properties: OPENROUTER_API_KEY=your_api_key_here
    // Or set environment variable: export OPENROUTER_API_KEY=your_api_key_here
    val apiKey = project.findProperty("OPENROUTER_API_KEY") as? String
        ?: System.getenv("OPENROUTER_API_KEY")

    buildConfigField("String", "OPENROUTER_API_KEY", "\"$apiKey\"")
    buildConfigField("String", "OPENROUTER_BASE_URL", "\"https://openrouter.ai/api/v1/\"")
    buildConfigField("String", "DEFAULT_MODEL", "\"qwen/qwen3-coder:free\"")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    with(libs.room.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosX64", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}

