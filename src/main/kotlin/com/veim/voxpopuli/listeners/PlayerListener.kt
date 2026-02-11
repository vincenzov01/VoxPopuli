package com.veim.voxpopuli.listeners

import com.hypixel.hytale.event.EventRegistry
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.veim.voxpopuli.database.UserServices
import java.util.logging.Level

/**
 * Listener for player connection events.
 */
class PlayerListener {
    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
    }

    /**
     * Register all player event listeners.
     */
    fun register(eventBus: EventRegistry) {
        // PlayerConnectEvent
        try {
            eventBus.register(PlayerConnectEvent::class.java, ::onPlayerConnect)
            LOGGER.at(Level.INFO).log("[VoxPopuli] Registered PlayerConnectEvent listener")
        } catch (e: Exception) {
            LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Failed to register PlayerConnectEvent")
        }

        // PlayerDisconnectEvent
        try {
            eventBus.register(PlayerDisconnectEvent::class.java, ::onPlayerDisconnect)
            LOGGER.at(Level.INFO).log("[VoxPopuli] Registered PlayerDisconnectEvent listener")
        } catch (e: Exception) {
            LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Failed to register PlayerDisconnectEvent")
        }
    }

    private fun onPlayerConnect(event: PlayerConnectEvent) {
        val playerRef = event.playerRef
        val playerName = playerRef?.username ?: "Unknown"
        val worldName = event.world?.name ?: "unknown"

        LOGGER.at(Level.INFO).log("[VoxPopuli] Player %s connected to world %s", playerName, worldName)

        // Crea l'utente solo se l'username è valido (non vuoto, almeno 3 caratteri, solo lettere/numeri/underscore)
        if (playerRef != null) {
            val username = playerRef.username
            val isValid = username.isNotBlank() && username.length >= 3 && username.matches(Regex("^[A-Za-z0-9_]+$"))
            if (isValid) {
                val existingUser = UserServices.getUserByUsername(username)
                if (existingUser == null) {
                    UserServices.createUser(username)
                    LOGGER.at(Level.INFO).log("[VoxPopuli] Nuovo utente creato nel database: %s", username)
                } else {
                    LOGGER.at(Level.INFO).log("[VoxPopuli] Utente già presente nel database: %s", username)
                }
            } else {
                LOGGER.at(Level.WARNING).log("[VoxPopuli] Username non valido: %s", username)
            }
        }
    }

    private fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        val playerName = event.playerRef?.username ?: "Unknown"

        LOGGER.at(Level.INFO).log("[VoxPopuli] Player %s disconnected", playerName)

        // TODO: Add your player leave logic here
    }
}
