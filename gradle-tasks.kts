// File: build.gradle.kts (aggiungi questo task alla fine)

// Task personalizzato per deploy automatico
tasks.register<Copy>("deployToServer") {
    description = "Copia il JAR nel server Hytale"
    group = "deployment"
    
    dependsOn("shadowJar")
    
    from(layout.buildDirectory.file("libs/VoxPopuli-${project.version}.jar"))
    into("C:/Users/music/Desktop/Hytale/Server/Server/Mods")
    
    doLast {
        println("========================================")
        println("  ✅ VoxPopuli deployato con successo!")
        println("========================================")
        println("Percorso: C:/Users/music/Desktop/Hytale/Server/Server/Mods")
        println("File: VoxPopuli-${project.version}.jar")
        println("")
        println("➡️  Riavvia il server Hytale")
        println("➡️  Usa /vox in-game")
    }
}

// Task per build rapido (senza relocate per sviluppo)
tasks.register<Jar>("devJar") {
    description = "Build veloce per sviluppo (senza shadow)"
    group = "build"
    
    archiveClassifier.set("dev")
    from(sourceSets.main.get().output)
    
    doLast {
        println("Dev JAR creato: ${archiveFile.get()}")
    }
}
