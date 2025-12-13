
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
            implementation(libs.ktor.client.websockets)
            api(libs.decompose)
            api(libs.decompose.compose)
            api(libs.essenty.coroutines)
            implementation(libs.kotlinx.serialization.json)
            api(libs.koin.core)
            api(libs.koin.compose)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.icons)
            implementation(libs.multiplatform.markdown.renderer.m3)
            implementation(libs.mcp.kotlin.client)
            implementation(libs.mcp.kotlin.core)
            implementation(libs.meeseeks)
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
            implementation(libs.lucene.core)
            implementation(libs.lucene.analyzers.common)
            implementation(libs.lucene.queryparser)
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
    buildConfigField("String", "OPENROUTER_BASE_URL", "\"http://localhost:11434/api/\"")
    buildConfigField("String", "DEFAULT_MODEL", "\"granite3.3:8b\"")
    val ollamaBaseUrl = project.findProperty("OLLAMA_BASE_URL") as? String
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434/api/"
    val chromaBaseUrl = project.findProperty("CHROMA_BASE_URL") as? String
        ?: System.getenv("CHROMA_BASE_URL")
        ?: "http://localhost:8000/api/v1/"
    val defaultEmbeddingModel = project.findProperty("EMBEDDING_MODEL") as? String
        ?: System.getenv("EMBEDDING_MODEL")
        ?: "nomic-embed-text"
    buildConfigField("String", "OLLAMA_BASE_URL", "\"$ollamaBaseUrl\"")
    buildConfigField("String", "CHROMA_BASE_URL", "\"$chromaBaseUrl\"")
    buildConfigField("String", "DEFAULT_EMBEDDING_MODEL", "\"$defaultEmbeddingModel\"")
    buildConfigField("String", "DEFAULT_RERANKER_MODEL", "\"dengcao/Qwen3-Reranker-0.6B:F16\"")
    buildConfigField("Double", "DEFAULT_VECTOR_WEIGHT", "1.0")
    buildConfigField("Double", "DEFAULT_LEXICAL_WEIGHT", "1.0")

    val luceneIndexPath = project.findProperty("LUCENE_INDEX_PATH") as? String
        ?: System.getenv("LUCENE_INDEX_PATH")
        ?: "./data/lucene-bm25-index"
    buildConfigField("String", "LUCENE_INDEX_PATH", "\"$luceneIndexPath\"")
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
