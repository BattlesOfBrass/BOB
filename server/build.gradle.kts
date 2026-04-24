plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
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
        name = "craftsblockReleases"
        url = uri("https://repo.craftsblock.de/experimental")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform("de.craftsblock.craftscore:bom:3.8.13-pre9"))
    implementation("de.craftsblock.craftscore:buffer")
    implementation("de.craftsblock.craftsnet.modules.websocketpackets:common:1.1.2-pre5")
    implementation("de.craftsblock.craftscore:event")

    implementation("com.google.code.gson:gson:2.13.2")

    // Source: https://mvnrepository.com/artifact/org.jetbrains/annotations
    implementation("org.jetbrains:annotations:26.1.0")
    annotationProcessor("org.jetbrains:annotations:26.1.0")

    implementation("com.google.guava:guava:33.5.0-jre")
    // Source: https://mvnrepository.com/artifact/it.unimi.dsi/fastutil
    implementation("it.unimi.dsi:fastutil:8.5.18")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    dependencies {
        include(project(":shared"))
    }
    archiveClassifier.set("")
    archiveBaseName.set("BOB-server")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "de.idiotischer.bob.Server"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) //centralize jdk ver from gradle.properties pwease
    }
}

tasks.jar {
    enabled = true //sonst geht BOB sderver import nd
}
