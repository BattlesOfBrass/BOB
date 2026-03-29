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
    implementation(platform("de.craftsblock.craftscore:bom:3.8.13-pre8"))

    implementation("de.craftsblock.craftscore:event")

    implementation("com.google.code.gson:gson:2.13.2")

    implementation(project(":shared"))

    implementation("org.jetbrains:annotations:26.1.0")
    annotationProcessor("org.jetbrains:annotations:26.1.0")

    // Source: https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.5.0-jre")
    // Source: https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.18")
}

tasks.build {
    dependsOn(tasks.shadowJar)
    // On Linux: shadowJar → jpackageMain → appimageMain
    // On Windows: shadowJar → jpackageMain
    dependsOn(if (isWindows) "jpackageMain" else "appimageMain")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))

    archiveBaseName.set("BOB-main")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "de.idiotischer.bob.BOB"
    }
}

val runOutputDir = layout.buildDirectory.dir("../run")

tasks.register("buildPreRun") {
    dependsOn(tasks.named("shadowJar"))

    group = "build"
    description = "Builds the shadowJar and moves it to the run dir"

    doLast {
        val jarFile = tasks.named("shadowJar").get().outputs.files.singleFile
        val destFolder = runOutputDir.get().asFile

        if (!destFolder.exists()) {
            destFolder.mkdirs()
        }

        val destFile = destFolder.resolve(jarFile.name)
        println("Moving ${jarFile.absolutePath} to ${destFile.absolutePath}")

        Files.copy(jarFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

// ── Native executable via jpackage ───────────────────────────────────────────
tasks.register<Exec>("jpackageMain") {
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
        "--name",        "BOB",
        "--app-version", jpackageVersion,
        "--input",       inputDir.absolutePath,
        "--main-jar",    "BOB-main.jar",
        "--icon",        iconFile.absolutePath,
        "--dest",        outputDir.absolutePath
    )
}

// ── AppImage (Linux only) ─────────────────────────────────────────────────────
tasks.register<Exec>("appimageMain") {
    group = "distribution"
    description = "Packages the jpackage app-image as a portable .AppImage (Linux only)"
    dependsOn(tasks.named("jpackageMain"))

    val appDir    = layout.buildDirectory.dir("jpackage/BOB").get().asFile
    val outputDir = layout.buildDirectory.dir("appimage").get().asFile
    val iconSrc   = rootProject.file("assets/logo/BOB Logo Large.png")

    doFirst {
        require(!isWindows) { "appimageMain is a Linux-only task." }
        outputDir.mkdirs()

        // Icon at AppDir root (required by AppImage spec)
        Files.copy(iconSrc.toPath(), appDir.resolve("BOB.png").toPath(), StandardCopyOption.REPLACE_EXISTING)

        // .desktop file
        appDir.resolve("BOB.desktop").writeText(
            """
            [Desktop Entry]
            Name=BOB
            Exec=BOB
            Icon=BOB
            Type=Application
            Categories=Game;
            """.trimIndent()
        )

        // AppRun script
        val appRun = appDir.resolve("AppRun")
        appRun.writeText(
            "#!/bin/bash\n" +
            "APPDIR=\"\$(dirname \"\$(readlink -f \"\$0\")\")\"\n" +
            "exec \"\$APPDIR/bin/BOB\" \"\$@\"\n"
        )
        appRun.setExecutable(true)
    }

    commandLine(
        "appimagetool",
        appDir.absolutePath,
        outputDir.resolve("BOB-$jpackageVersion-x86_64.AppImage").absolutePath
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))  //centralize jdk ver from gradle.properties pwease
    }
}

tasks.jar {
    enabled = false
}
