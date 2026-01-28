package com.voxpopuli.commands

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.World
import com.voxpopuli.ui.VoxMainPage

class VoxCommand : AbstractPlayerCommand("vox") {

    override fun getName(): String = "vox"

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        VoxMainPage.open(playerRef, store)
    }
}