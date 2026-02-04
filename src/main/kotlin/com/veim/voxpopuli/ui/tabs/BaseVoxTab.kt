package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage

abstract class BaseVoxTab(
    final override val id: String,
    final override val title: String,
) : VoxTab {

    final override fun apply(cmd: UICommandBuilder) {
        cmd.set("#ContentTitle.Text", title)
    }

    override fun bindEvents(page: VoxPopuliDashboardPage, evt: UIEventBuilder) {
        // Default: no-op
    }

    override fun render(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    ) {
        // Default: no-op
    }

    override fun handleEvent(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: VoxPopuliDashboardPage.EventData,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    ): Boolean = false

    protected fun getOrCreateUser(page: VoxPopuliDashboardPage): User? {
        val username = page.player.username
        return UserServices.getUserByUsername(username) ?: UserServices.createUser(username)
    }
}
