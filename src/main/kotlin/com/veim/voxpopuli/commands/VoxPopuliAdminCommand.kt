package com.veim.voxpopuli.commands

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.VoxPopuliPlugin
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.OpPermissionsUtil

class VoxPopuliAdminCommand(private val plugin: VoxPopuliPlugin) : AbstractPlayerCommand("voxadmin", "Apri la dashboard admin VoxPopuli") {
    override fun canGeneratePermission(): Boolean = false

    init {
        // Variant: /voxadmin add <username> | /voxadmin remove <username>
        addUsageVariant(ManageAdminVariant(plugin))
    }

    override fun execute(
        commandContext: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        if (player == null) {
            commandContext.sendMessage(Message.raw("Impossibile aprire la dashboard admin: player non trovato."))
            return
        }

        val username = playerRef.username
        val isAdmin = UserServices.getUserByUsername(username)?.isAdmin == true
        val isOp = player.getUuid()?.let { OpPermissionsUtil.isOp(it) } == true
        if (!isAdmin && !isOp) {
            commandContext.sendMessage(Message.raw("Non hai i permessi per usare /voxadmin."))
            return
        }

        player.pageManager.openCustomPage(ref, store, VoxPopuliAdminDashboardPage(playerRef, plugin))
    }

    private class ManageAdminVariant(private val plugin: VoxPopuliPlugin) : AbstractPlayerCommand(
        "Gestione admin VoxPopuli"
    ) {
        private val actionArg: RequiredArg<String>
        private val usernameArg: RequiredArg<String>

        init {
            actionArg = withRequiredArg("action", "add/remove", ArgTypes.STRING)
            usernameArg = withRequiredArg("username", "Username target", ArgTypes.STRING)
        }

        override fun canGeneratePermission(): Boolean = false

        override fun execute(
            commandContext: CommandContext,
            store: Store<EntityStore>,
            ref: Ref<EntityStore>,
            playerRef: PlayerRef,
            world: World
        ) {
            val player = store.getComponent(ref, Player.getComponentType())
            if (player == null) {
                commandContext.sendMessage(Message.raw("Comando non disponibile: player non trovato."))
                return
            }

            val isOp = player.getUuid()?.let { OpPermissionsUtil.isOp(it) }
            if (isOp != true) {
                val reason = if (isOp == null) {
                    " (impossibile verificare OP: permissions.json non trovato)"
                } else {
                    ""
                }
                commandContext.sendMessage(Message.raw("Non hai i permessi per gestire gli admin con /voxadmin.$reason"))
                return
            }

            val action = actionArg.get(commandContext)?.trim().orEmpty().lowercase()
            val targetUsername = usernameArg.get(commandContext)?.trim().orEmpty()
            if (targetUsername.isBlank()) {
                commandContext.sendMessage(Message.raw("Uso: /voxadmin add <username> | /voxadmin remove <username>"))
                return
            }

            val makeAdmin = when (action) {
                "add" -> true
                "remove" -> false
                else -> {
                    commandContext.sendMessage(Message.raw("Azione non valida '$action'. Uso: /voxadmin add <username> | /voxadmin remove <username>"))
                    return
                }
            }

            val target = UserServices.getUserByUsername(targetUsername) ?: UserServices.createUser(targetUsername)
            if (target == null) {
                commandContext.sendMessage(Message.raw("Impossibile creare/trovare l'utente '$targetUsername'."))
                return
            }

            val ok = UserServices.updateUser(target.copy(isAdmin = makeAdmin))
            if (!ok) {
                commandContext.sendMessage(Message.raw("Errore aggiornando i permessi admin per '${target.username}'."))
                return
            }

            val status = if (makeAdmin) "ADMIN" else "NON-ADMIN"
            commandContext.sendMessage(Message.raw("${target.username} impostato come $status."))
        }
    }
}
