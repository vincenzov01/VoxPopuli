package com.veim.voxpopuli

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.logger.HytaleLogger
import com.veim.voxpopuli.commands.VoxPopuliPluginCommand
import com.veim.voxpopuli.config.VoxPopuliConfigManager
import com.veim.voxpopuli.database.DatabaseManager
import java.util.logging.Level

/**
 * VoxPopuli - A Hytale server plugin.
 */
class VoxPopuliPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }

    private val configManager: VoxPopuliConfigManager = VoxPopuliConfigManager(VoxPopuliConfigManager.defaultPath())

    init {
        // The Hytale API can be strict about when config access happens; keep this in the constructor.
        configManager.ensureExists()
        configManager.load()
    }

    override fun setup() {
        // Per la documentazione Hytale: setup() è il posto corretto per inizializzare
        // le risorse del plugin (senza fare integrazioni cross-plugin pesanti).
        LOGGER.at(Level.INFO).log("[VoxPopuli] Setting up...")

        // Ensure config file exists and is up-to-date on disk.
        configManager.save()
        LOGGER.at(Level.INFO).log("[VoxPopuli] Config path: %s", configManager.path().toAbsolutePath().toString())

        // Inizializza il database (Exposed/SQLite).
        DatabaseManager.init(configManager.get().database.path)

        // Il plugin deve collegarsi al comando: registriamo solo un comando base.
        registerCommands()

        LOGGER.at(Level.INFO).log("[VoxPopuli] Setup complete!")
    }

    private fun registerCommands() {
        try {
            commandRegistry.registerCommand(VoxPopuliPluginCommand())
            LOGGER.at(Level.INFO).log("[VoxPopuli] Registered /vox command")
        } catch (e: Exception) {
            LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Failed to register commands")
        }
    }

    override fun start() {
        // start() viene chiamato dopo che TUTTI i plugin hanno completato setup().
        LOGGER.at(Level.INFO).log("[VoxPopuli] Started!")
    }

    override fun shutdown() {
        LOGGER.at(Level.INFO).log("[VoxPopuli] Shutting down...")
    }
}