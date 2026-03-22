plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "de.idiotischer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "craftsblockReleases"
        url = uri("https://repo.craftsblock.de/experimental")
    }
}

dependencies {
    // Source: https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation(platform("de.craftsblock.craftscore:bom:3.8.13-pre8"))
    implementation("de.craftsblock.craftscore:event")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation(project(":shared"))
}

tasks.shadowJar {
    // (shared + gson + craftscore)
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    archiveClassifier.set("")
    archiveBaseName.set("BOB-main")
    manifest {
        attributes["Main-Class"] = "de.idiotischer.bob.BOB"
    }
}

tasks.jar {
    enabled = false
}
