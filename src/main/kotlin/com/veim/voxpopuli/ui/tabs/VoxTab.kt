package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage

interface VoxTab {
    val id: String
    val title: String
    val templatePath: String

    /** Applies this tab's base UI state (title, etc.). */
    fun apply(cmd: UICommandBuilder)

    /** Adds event bindings for controls that belong to this tab. */
    fun bindEvents(page: VoxPopuliDashboardPage, evt: UIEventBuilder)

    /** Renders/refreshes this tab content into the provided UICommandBuilder. */
    fun render(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    )

    /**
     * Handles an incoming UI event. Returns true if handled.
     * Tabs may choose to not send updates (e.g., draft capture) by returning true and letting the caller decide.
     */
    fun handleEvent(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: VoxPopuliDashboardPage.EventData,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    ): Boolean
}
