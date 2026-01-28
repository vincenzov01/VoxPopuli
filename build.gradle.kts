// File: build.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
    }
}

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.voxpopuli"
version = "1.0.0"

repositories {
    mavenCentral()
    
    // CurseMaven for HyUI
    maven {
        name = "cursemaven"
        url = uri("https://www.cursemaven.com")
    }
}

dependencies {
    // Hytale Server API - path locale come nel pom.xml
    compileOnly(files("C:/Users/music/Desktop/Hytale/Server/Server/HytaleServer.jar"))
    
    // HyUI - UI Library  
    compileOnly("curse.maven:hyui-1431415:7537692")

    // Exposed ORM + SQLite
    implementation("org.jetbrains.exposed:exposed-core:0.46.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.46.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.46.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// ========================================
// CONFIGURAZIONE SHADOW JAR
// ========================================

tasks.jar {
    enabled = false
}

tasks.processResources {

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // Forza Gradle a processare tutti i file nelle risorse
    from("src/main/resources") {
        include("**/*.html")
        include("**/*.json")
        include("**/*.ui")
    }

    filesMatching("manifest.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
    archiveFileName.set("VoxPopuli-${project.version}.jar")

    // Assicurati che le risorse processate siano incluse nel JAR finale
    from(tasks.processResources)

    // Escludi file inutili che appesantiscono il JAR
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

// ========================================
// TASK PERSONALIZZATI
// ========================================

tasks.register<Copy>("deployToServer") {
    description = "Copia il JAR compilato nel server Hytale"
    group = "deployment"
    
    dependsOn(tasks.named("shadowJar"))
    
    from(layout.buildDirectory.file("libs/VoxPopuli-${project.version}.jar"))
    into("C:/Users/music/Desktop/Hytale/Server/Server/Mods")
    
    doLast {
        println("")
        println("========================================")
        println("  ✅ VoxPopuli deployato con successo!")
        println("========================================")
        println("📁 Percorso: C:/Users/music/Desktop/Hytale/Server/Server/Mods")
        println("📦 File: VoxPopuli-${project.version}.jar")
        println("")
        println("🔄 Riavvia il server Hytale")
        println("🎮 Usa /vox in-game")
        println("========================================")
    }
}

tasks.register("buildAndDeploy") {
    description = "Build completo e deploy nel server"
    group = "deployment"
    
    dependsOn(tasks.clean, tasks.named("deployToServer"))
    
    doLast {
        println("✨ Build e deploy completati!")
    }
}
