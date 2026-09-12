import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
    `java-library`
}

group = "de.idiotischer"
version = property("appVersion") as String

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
        name = "cbr"
        url = uri("https://repo.craftsblock.de/releases")
    }

    maven {
        name = "cbe"
        url = uri("https://repo.craftsblock.de/experimental")
    }

    //maven {
    //    url = uri("https://releases.aspose.com/java/repo/")
    //}
}

dependencies {
    implementation(platform("de.craftsblock.craftscore:bom:3.8.17"))
    implementation("de.craftsblock.craftscore:buffer")
    implementation("de.craftsblock.craftsnet.modules.websocketpackets:common:1.1.2-pre5")
    implementation("de.craftsblock.craftscore:event")

    implementation("at.yawk.lz4:lz4-java:1.11.0")
    implementation("com.github.gotson:webp-imageio:0.2.2")

    implementation("com.google.code.gson:gson:2.13.2")

    api(project(":shared"))
    implementation(project(":server"))

    implementation("org.jetbrains:annotations:26.1.0")
    annotationProcessor("org.jetbrains:annotations:26.1.0")

    // Source: https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.5.0-jre")
    // Source: https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.18")
    // Source: https://mvnrepository.com/artifact/com.aspose/aspose-psd
    //implementation("com.aspose:aspose-psd:26.5:jdk16") we dont use it currently so yeah
}

tasks.build {
    dependsOn(tasks.shadowJar)

    dependsOn(if (isWindows) "jpackageMain" else "appimageMain")
}

tasks.shadowJar {
    //configurations = listOf(project.configurations.runtimeClasspath.get())
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
val args = listOf(
    "--enable-native-access=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"
)


tasks.withType<JavaExec>().configureEach {
    jvmArgs(args)
}

tasks.register<Exec>("jpackageMain") {
    group = "distribution"
    description = "Creates a native app-image for the current platform using jpackage"
    dependsOn(tasks.named("shadowJar"))

    val jpackageBin = if (isWindows)
        "${System.getProperty("java.home")}\\bin\\jpackage.exe"
    else
        "${System.getProperty("java.home")}/bin/jpackage"

    val inputDir = layout.buildDirectory.dir("libs").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile

    val iconFile = if (isWindows)
        rootProject.file("assets/logo/BOB_logo.ico")
    else
        rootProject.file("assets/logo/BOB_logo_large.png")

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
        "--type", "app-image",
        "--name", "BOB",
        "--app-version", jpackageVersion,
        "--input", inputDir.absolutePath,
        "--main-jar", "BOB-main.jar",
        "--icon", iconFile.absolutePath,
        "--dest", outputDir.absolutePath,

        *args.flatMap { listOf("--java-options", it) }.toTypedArray()
    )
}


tasks.register<Exec>("appimageMain") {
    group = "distribution"
    description = "Packages the jpackage app-image as a portable .AppImage (Linux only)"
    dependsOn(tasks.named("jpackageMain"))

    val appDir    = layout.buildDirectory.dir("jpackage/BOB").get().asFile
    val outputDir = layout.buildDirectory.dir("appimage").get().asFile
    val iconSrc   = rootProject.file("assets/logo/BOB_logo_large.png")

    doFirst {
        require(!isWindows) { "appimageMain is a Linux-only task." }
        outputDir.mkdirs()

        Files.copy(iconSrc.toPath(), appDir.resolve("BOB.png").toPath(), StandardCopyOption.REPLACE_EXISTING)

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

        val appRun = appDir.resolve("AppRun")
        appRun.writeText(
            "#!/bin/bash\n" +
                    "APPDIR=\"\$(dirname \"\$(readlink -f \"\$0\")\")\"\n" +
                    "export JAVA_TOOL_OPTIONS=\"--enable-native-access=ALL-UNNAMED\"\n" +
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

tasks.register<JavaExec>("runApp") {
    dependsOn("buildPreRun")

    group = "run"

    val runDir = runOutputDir.get().asFile
    val jarFile = runDir.resolve(tasks.shadowJar.get().archiveFileName.get())

    workingDir = runDir
    classpath = files(jarFile)
    mainClass.set("de.idiotischer.bob.BOB")

    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"
    )
}
tasks.jar {
    enabled = false
}