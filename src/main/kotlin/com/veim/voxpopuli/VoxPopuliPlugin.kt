package com.veim.voxpopuli

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.logger.HytaleLogger
import com.veim.voxpopuli.commands.VoxPopuliPluginCommand
import com.veim.voxpopuli.commands.VoxPopuliAdminCommand
import com.veim.voxpopuli.config.VoxPopuliConfig
import com.veim.voxpopuli.config.VoxPopuliDbConfigStore
import com.veim.voxpopuli.database.DatabaseManager
import java.util.logging.Level

/**
 * VoxPopuli - A Hytale server plugin.
 */
class VoxPopuliPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }

    private val dbPath: String = VoxPopuliConfig().database.path

    @Volatile
    private var runtimeConfig: VoxPopuliConfig = VoxPopuliConfig()


    fun config(): VoxPopuliConfig = runtimeConfig

    /** Aggiorna la config dei log custom ogni volta che la config viene caricata o aggiornata */
    private fun updateLogConfig() {
        com.veim.voxpopuli.util.FileAuditLog.updateConfig(runtimeConfig.logs)
    }

    fun saveConfig(config: VoxPopuliConfig) {
        // Single source of truth: DB.
        // Keep db path consistent even if callers mutate it.
        val toSave = config.copy(database = config.database.copy(path = dbPath))
        runtimeConfig = toSave
        VoxPopuliDbConfigStore.save(toSave)
        updateLogConfig()
    }

    init {
        // No-op: config is DB-only.
    }

    override fun setup() {
        // Per la documentazione Hytale: setup() è il posto corretto per inizializzare
        // le risorse del plugin (senza fare integrazioni cross-plugin pesanti).
        LOGGER.at(Level.INFO).log("[VoxPopuli] Setting up...")

        // Inizializza il database (Exposed/SQLite).
        DatabaseManager.init(dbPath)

        // Load config from DB (or seed from defaults).
        val loaded = VoxPopuliDbConfigStore.loadOrInit(VoxPopuliConfig())
        runtimeConfig = loaded.config.copy(database = loaded.config.database.copy(path = dbPath))
        updateLogConfig()

        // Il plugin deve collegarsi al comando: registriamo solo un comando base.
        registerCommands()

        LOGGER.at(Level.INFO).log("[VoxPopuli] Setup complete!")
    }

    private fun registerCommands() {
        try {
            commandRegistry.registerCommand(VoxPopuliPluginCommand(this))
            LOGGER.at(Level.INFO).log("[VoxPopuli] Registered /vox command")

            commandRegistry.registerCommand(VoxPopuliAdminCommand(this))
            LOGGER.at(Level.INFO).log("[VoxPopuli] Registered /voxadmin command")
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