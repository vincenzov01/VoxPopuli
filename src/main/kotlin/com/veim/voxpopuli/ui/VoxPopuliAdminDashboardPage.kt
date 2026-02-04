package com.veim.voxpopuli.ui

import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class VoxPopuliAdminDashboardPage(playerRef: PlayerRef) : InteractiveCustomUIPage<VoxPopuliAdminDashboardPage.EventData>(
    playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC
) {
    override fun build(
        ref: Ref<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        // TODO: Implement admin dashboard UI logic
        cmd.append("voxpopuli/AdminDashboard.ui")
    }

    data class EventData(
        var action: String = "tab",
        var tab: String = ""
    ) {
        companion object {
            val CODEC = com.hypixel.hytale.codec.builder.BuilderCodec.builder(EventData::class.java, ::EventData)
                .append(com.hypixel.hytale.codec.KeyedCodec("Action", com.hypixel.hytale.codec.Codec.STRING), { e, v -> e.action = v }, { e -> e.action }).add()
                .append(com.hypixel.hytale.codec.KeyedCodec("Tab", com.hypixel.hytale.codec.Codec.STRING), { e, v -> e.tab = v }, { e -> e.tab }).add()
                .build()
        }
    }
}
