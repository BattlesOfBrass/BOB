import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
}

group = "de.idiotischer"
version = property("appVersion") as String

// jpackage requires a clean semver (no "-SNAPSHOT" suffix)
val jpackageVersion = (version as String).substringBefore("-")

val isWindows = System.getProperty("os.name").lowercase().contains("windows")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "craftsblockReleases"
        url = uri("https://repo.craftsblock.de/experimental")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("de.craftsblock.craftscore:bom:3.8.13-pre8"))
    implementation("de.craftsblock.craftscore:event")
    implementation("com.google.code.gson:gson:2.13.2")

    // Source: https://mvnrepository.com/artifact/org.jetbrains/annotations
    implementation("org.jetbrains:annotations:26.1.0")
    annotationProcessor("org.jetbrains:annotations:26.1.0")

    implementation("com.google.guava:guava:33.5.0-jre")
    // Source: https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.18")
}

tasks.build {
    // On Linux: shadowJar → jpackageServer → appimageServer
    // On Windows: shadowJar → jpackageServer
    dependsOn(if (isWindows) "jpackageServer" else "appimageServer")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    dependencies {
        include(project(":shared"))
    }
    archiveClassifier.set("")
    archiveBaseName.set("BOB-server")
    archiveVersion.set("")   // predictable filename: BOB-server.jar
    manifest {
        attributes["Main-Class"] = "de.idiotischer.bob.Server"
    }
}

// ── Native executable via jpackage ───────────────────────────────────────────
tasks.register<Exec>("jpackageServer") {
    group = "distribution"
    description = "Creates a native app-image for the current platform using jpackage"
    dependsOn(tasks.named("shadowJar"))

    val jpackageBin = if (isWindows)
        "${System.getProperty("java.home")}\\bin\\jpackage.exe"
    else
        "${System.getProperty("java.home")}/bin/jpackage"

    val inputDir  = layout.buildDirectory.dir("libs").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile

    val iconFile = if (isWindows)
        rootProject.file("assets/logo/BOB Logo.ico")
    else
        rootProject.file("assets/logo/BOB Logo Large.png")

    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        require(iconFile.exists()) {
            "Icon not found at ${iconFile.absolutePath}. " +
            "On Windows, run the 'Convert icon to ICO' workflow step first."
        }
    }

    commandLine(
        jpackageBin,
        "--type",        "app-image",
        "--name",        "BOB-server",
        "--app-version", jpackageVersion,
        "--input",       inputDir.absolutePath,
        "--main-jar",    "BOB-server.jar",
        "--icon",        iconFile.absolutePath,
        "--dest",        outputDir.absolutePath
    )
}

// ── AppImage (Linux only) ─────────────────────────────────────────────────────
tasks.register<Exec>("appimageServer") {
    group = "distribution"
    description = "Packages the jpackage app-image as a portable .AppImage (Linux only)"
    dependsOn(tasks.named("jpackageServer"))

    val appDir    = layout.buildDirectory.dir("jpackage/BOB-server").get().asFile
    val outputDir = layout.buildDirectory.dir("appimage").get().asFile
    val iconSrc   = rootProject.file("assets/logo/BOB Logo Large.png")

    doFirst {
        require(!isWindows) { "appimageServer is a Linux-only task." }
        outputDir.mkdirs()

        // Icon at AppDir root (required by AppImage spec)
        Files.copy(iconSrc.toPath(), appDir.resolve("BOB-server.png").toPath(), StandardCopyOption.REPLACE_EXISTING)

        // .desktop file
        appDir.resolve("BOB-server.desktop").writeText(
            """
            [Desktop Entry]
            Name=BOB Server
            Exec=BOB-server
            Icon=BOB-server
            Type=Application
            Categories=Network;
            """.trimIndent()
        )

        // AppRun script
        val appRun = appDir.resolve("AppRun")
        appRun.writeText(
            "#!/bin/bash\n" +
            "APPDIR=\"\$(dirname \"\$(readlink -f \"\$0\")\")\"\n" +
            "exec \"\$APPDIR/bin/BOB-server\" \"\$@\"\n"
        )
        appRun.setExecutable(true)
    }

    commandLine(
        "appimagetool",
        appDir.absolutePath,
        outputDir.resolve("BOB-server-$jpackageVersion-x86_64.AppImage").absolutePath
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) //centralize jdk ver from gradle.properties pwease
    }
}

tasks.jar {
    enabled = false
}
