package com.voxpopuli

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.logger.HytaleLogger
import com.voxpopuli.commands.VoxCommand
import com.voxpopuli.managers.DatabaseManager
import javax.annotation.Nonnull
import java.io.File

class VoxPopuliPlugin(@Nonnull init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()

        lateinit var instance: VoxPopuliPlugin
            private set
    }

    override fun setup() {
        instance = this

        // Creazione cartella dati
        val pluginDataFolder = File("plugins/VoxPopuli")
        if (!pluginDataFolder.exists()) {
            pluginDataFolder.mkdirs()
        }

        // Inizializzazione Database
        try {
            // Rimosso il costruttore: DatabaseManager è un 'object', lo chiamiamo direttamente
            DatabaseManager.connect()
            LOGGER.atInfo().log("⚜️ VoxPopuli: Database connesso correttamente.")
        } catch (e: Exception) {
            LOGGER.atSevere().withCause(e).log("❌ VoxPopuli: Errore inizializzazione database!")
        }

        // Registrazione Comando
        this.commandRegistry.registerCommand(VoxCommand())

        LOGGER.atInfo().log("========================================")
        LOGGER.atInfo().log("  ⚜️ VoxPopuli v1.0.0 - PRONTO")
        LOGGER.atInfo().log("========================================")
    }

    // In Hytale JavaPlugin, onEnable e onDisable non sono override obbligatori,
    // ma setup() è il punto d'ingresso principale.
}