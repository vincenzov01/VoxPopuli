package com.veim.voxpopuli.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

class VoxPopuliConfigManager(
    private val configPath: Path,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create(),
) {
    @Volatile
    private var cached: VoxPopuliConfig = VoxPopuliConfig()

    fun path(): Path = configPath

    fun get(): VoxPopuliConfig = cached

    /** Ensures the config exists on disk, without overwriting existing values. */
    fun ensureExists() {
        if (Files.exists(configPath)) return
        save(VoxPopuliConfig())
    }

    fun load(): VoxPopuliConfig {
        if (!Files.exists(configPath)) {
            cached = VoxPopuliConfig()
            return cached
        }

        val json = Files.readString(configPath, StandardCharsets.UTF_8)
        cached = try {
            gson.fromJson(json, VoxPopuliConfig::class.java) ?: VoxPopuliConfig()
        } catch (e: JsonSyntaxException) {
            backupCorruptFile()
            VoxPopuliConfig()
        } catch (e: Exception) {
            // Any other IO/parsing edge: keep defaults.
            VoxPopuliConfig()
        }

        return cached
    }

    fun save(config: VoxPopuliConfig = cached) {
        cached = config
        Files.createDirectories(configPath.parent)
        Files.writeString(configPath, gson.toJson(config), StandardCharsets.UTF_8)
    }

    private fun backupCorruptFile() {
        try {
            val backupName = configPath.fileName.toString() + ".bad-" + Instant.now().epochSecond
            val backupPath = configPath.resolveSibling(backupName)
            Files.move(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) {
            // Ignore backup errors.
        }
    }

    companion object {
        private const val DEFAULT_FILE_NAME = "config.json"
        private const val ENV_CONFIG_PATH = "VOXPOPULI_CONFIG_PATH"
        private const val DEFAULT_DIR_NAME = "voxpopuli"

        /**
         * Resolve config path.
         * - If VOXPOPULI_CONFIG_PATH is set, uses that.
         * - Otherwise uses current working directory.
         */
        fun defaultPath(): Path {
            val env = System.getenv(ENV_CONFIG_PATH)
            if (!env.isNullOrBlank()) return Path.of(env)

            // Hytale servers commonly keep mod configs under a top-level 'config' directory.
            val baseDir = Path.of(System.getProperty("user.dir"))
            val configDir = baseDir.resolve("config").resolve(DEFAULT_DIR_NAME)
            return configDir.resolve(DEFAULT_FILE_NAME)
        }
    }
}
