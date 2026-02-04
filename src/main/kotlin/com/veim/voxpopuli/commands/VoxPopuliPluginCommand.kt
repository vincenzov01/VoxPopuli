package com.veim.voxpopuli.commands

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage

class VoxPopuliPluginCommand : AbstractPlayerCommand("vox", "Apri la dashboard VoxPopuli") {
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
            commandContext.sendMessage(Message.raw("Impossibile aprire la dashboard: player non trovato."))
            return
        }

        // AbstractPlayerCommand runs on the world thread: safe to access Store/Refs.
        player.pageManager.openCustomPage(ref, store, VoxPopuliDashboardPage(playerRef))
    }
}