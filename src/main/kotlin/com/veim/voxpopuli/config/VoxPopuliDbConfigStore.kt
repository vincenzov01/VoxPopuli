package com.veim.voxpopuli.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import com.veim.voxpopuli.database.VoxPopuliPluginConfig

/**
 * Stores VoxPopuli configuration in SQLite.
 *
 * Why: keeping config in DB allows runtime changes without rewriting the commented config file.
 */
object VoxPopuliDbConfigStore {
	private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

	private fun deepMerge(base: JsonElement, override: JsonElement): JsonElement {
		if (base is JsonObject && override is JsonObject) {
			val merged = JsonObject()
			// start with base
			for ((key, baseVal) in base.entrySet()) {
				merged.add(key, baseVal)
			}
			// override / recurse
			for ((key, overrideVal) in override.entrySet()) {
				val existing = merged.get(key)
				if (existing != null) {
					merged.add(key, deepMerge(existing, overrideVal))
				} else {
					merged.add(key, overrideVal)
				}
			}
			return merged
		}
		// If override is not an object (or types differ), override wins.
		return override
	}

	data class Loaded(
		val config: VoxPopuliConfig,
		val updatedAtEpochSeconds: Long,
	)

	/**
	 * Loads config from DB or creates it from defaults if missing.
	 */
	fun loadOrInit(defaults: VoxPopuliConfig = VoxPopuliConfig()): Loaded {
		return transaction {
			val row = VoxPopuliPluginConfig.selectAll().limit(1).firstOrNull()
			if (row == null) {
				val now = Instant.now().epochSecond
				VoxPopuliPluginConfig.insert {
					it[VoxPopuliPluginConfig.id] = 1
					it[VoxPopuliPluginConfig.json] = gson.toJson(defaults)
					it[VoxPopuliPluginConfig.updatedAtEpochSeconds] = now
				}
				return@transaction Loaded(defaults, now)
			}

			val json = row[VoxPopuliPluginConfig.json]
			val updatedAt = row[VoxPopuliPluginConfig.updatedAtEpochSeconds]

			val config = try {
				// Merge with defaults to support config schema evolution (missing fields).
				val baseTree = gson.toJsonTree(defaults)
				val loadedTree = gson.fromJson(json, JsonElement::class.java)
				val merged = deepMerge(baseTree, loadedTree)
				gson.fromJson(merged, VoxPopuliConfig::class.java) ?: defaults
			} catch (_: JsonSyntaxException) {
				defaults
			} catch (_: Exception) {
				defaults
			}

			Loaded(config, updatedAt)
		}
	}

	fun save(config: VoxPopuliConfig) {
		transaction {
			val now = Instant.now().epochSecond
			val updated = VoxPopuliPluginConfig.update({ VoxPopuliPluginConfig.id eq 1 }) {
				it[VoxPopuliPluginConfig.json] = gson.toJson(config)
				it[VoxPopuliPluginConfig.updatedAtEpochSeconds] = now
			}
			if (updated == 0) {
				VoxPopuliPluginConfig.insert {
					it[VoxPopuliPluginConfig.id] = 1
					it[VoxPopuliPluginConfig.json] = gson.toJson(config)
					it[VoxPopuliPluginConfig.updatedAtEpochSeconds] = now
				}
			}
		}
	}
}
