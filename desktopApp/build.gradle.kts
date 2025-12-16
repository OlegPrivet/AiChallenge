import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.hot.reload)
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(compose.ui)
}

// Create custom task to copy Vosk model
val copyVoskModel = tasks.register<Exec>("copyVoskModel") {
    val sourceDir = project.layout.projectDirectory.dir("appResources").asFile.absolutePath
    val targetDir = project.layout.buildDirectory.dir("compose/binaries/main/app/${rootProject.name}.app/Contents/Resources").get().asFile.absolutePath

    commandLine("cp", "-r", sourceDir + "/.", targetDir)

    doFirst {
        println("Copying Vosk model from $sourceDir to $targetDir...")
        File(targetDir).mkdirs()
    }
    doLast {
        println("Vosk model copied successfully")
    }
}

// Make createDistributable depend on copyVoskModel
tasks.matching { it.name == "createDistributable" }.configureEach {
    dependsOn(copyVoskModel)
}

// Make packageDistributionForCurrentOS depend on copyVoskModel
tasks.matching { it.name == "packageDistributionForCurrentOS" }.configureEach {
    dependsOn(copyVoskModel)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // Set JVM args to point to resources directory during development
        jvmArgs(
            "-Dcompose.application.resources.dir=${project.layout.projectDirectory.dir("appResources").asFile.absolutePath}"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AiChallenge"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "org.oleg.ai.challenge.desktopApp"

                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>Speech recognition requires microphone access</string>
                    """
                }
            }
        }
    }
}
