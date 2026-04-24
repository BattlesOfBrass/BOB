import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
    `java-library`
}

group = "de.idiotischer"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "cbe"
        url = uri("https://repo.craftsblock.de/experimental")
    }
}

dependencies {
    implementation(platform("de.craftsblock.craftscore:bom:3.8.13-pre9"))
    implementation("de.craftsblock.craftscore:buffer")
    implementation("de.craftsblock.craftsnet.modules.websocketpackets:common:1.1.2-pre5")
    implementation("de.craftsblock.craftscore:event")

    implementation("com.google.code.gson:gson:2.13.2")

    api(project(":shared"))
    implementation(project(":server"))

    implementation("org.jetbrains:annotations:26.1.0")
    annotationProcessor("org.jetbrains:annotations:26.1.0")

    // Source: https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.5.0-jre")
    // Source: https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.18")
}

tasks.build {
    dependsOn(tasks.shadowJar)
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))  //centralize jdk ver from gradle.properties pwease
    }
}

tasks.jar {
    enabled = false
}