package com.veim.voxpopuli.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.VoxPopuliPlugin
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.tabs.*
import com.veim.voxpopuli.util.OpPermissionsUtil
import java.util.logging.Level
import com.hypixel.hytale.server.core.ui.builder.EventData as UIEventData

class VoxPopuliAdminDashboardPage(
    playerRef: PlayerRef,
) : InteractiveCustomUIPage<VoxPopuliAdminDashboardPage.EventData>(
        playerRef,
        CustomPageLifetime.CanDismiss,
        EventData.CODEC,
    ) {
    // Variabili membro
    lateinit var plugin: VoxPopuliPlugin
    private lateinit var tabs: Map<String, AdminTab>

    val player: PlayerRef = playerRef
    var activeTabId: String = TAB_CONFIG
    var statusMessage: String = ""

    constructor(playerRef: PlayerRef, plugin: VoxPopuliPlugin) : this(playerRef) {
        this.plugin = plugin
        this.tabs =
            mapOf(
                TAB_CONFIG to ConfigTab(this, plugin),
                TAB_LOGS to LogsTab(this),
                TAB_POSTS to PostsTab(this),
                TAB_MESSAGES to MessagesTab(this),
                TAB_GUILDS to GuildsTab(this),
            )
    }

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
        const val TAB_CONFIG: String = "config"
        const val TAB_LOGS: String = "logs"
        const val TAB_POSTS: String = "posts"
        const val TAB_MESSAGES: String = "messages"
        const val TAB_GUILDS: String = "guilds"
    }

    // Helper pubblico utilizzato dai tab
    fun uiEventData(
        action: String,
        tab: String? = null,
        id: Int? = null,
        guildId: Int? = null,
        userId: Int? = null,
        captures: Map<String, String> = emptyMap(),
    ): UIEventData {
        var data = UIEventData.of("Action", action)
        data = data.append("Tab", tab ?: activeTabId)
        if (id != null) data = data.append("Id", id.toString())
        if (guildId != null) data = data.append("GuildId", guildId.toString())
        if (userId != null) data = data.append("UserId", userId.toString())
        for ((key, selector) in captures) {
            data = data.append("@$key", selector)
        }
        return data
    }

    private fun buildLayout(cmd: UICommandBuilder) {
        cmd.append("voxpopuli/AdminDashboard.ui")
    }

    private fun renderHeader(cmd: UICommandBuilder) {
        cmd.set("#Username.Text", player.username)
        cmd.set("#RoleRole.Text", "Admin")
        cmd.set("#ContentTitle.Text", "VoxPopuli Admin")
        cmd.set("#SaveStatusLabel.Text", statusMessage)

        // Visibility containers
        cmd.set("#ConfigContent.Visible", activeTabId == TAB_CONFIG)
        cmd.set("#LogsContent.Visible", activeTabId == TAB_LOGS)
        cmd.set("#PostsContent.Visible", activeTabId == TAB_POSTS)
        cmd.set("#MessagesContent.Visible", activeTabId == TAB_MESSAGES)
        cmd.set("#GuildsContent.Visible", activeTabId == TAB_GUILDS)
    }

    override fun build(
        ref: Ref<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
        store: Store<EntityStore>,
    ) {
        buildLayout(cmd)

        // Binding Tabs statici
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabConfig", uiEventData(action = "tab", tab = TAB_CONFIG), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabLogs", uiEventData(action = "tab", tab = TAB_LOGS), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabPosts", uiEventData(action = "tab", tab = TAB_POSTS), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabMessages", uiEventData(action = "tab", tab = TAB_MESSAGES), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TabGuilds", uiEventData(action = "tab", tab = TAB_GUILDS), false)

        renderHeader(cmd)

        // Delega il render al tab attivo
        tabs[activeTabId]?.render(cmd, evt)
    }

    override fun handleDataEvent(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: EventData,
    ) {
        LOGGER.at(Level.INFO).log("[VoxPopuliAdmin] UI event: action=%s tab=%s", data.action, data.tab)

        val user = UserServices.getUserByUsername(player.username)
        if (user == null) {
            statusMessage = "Errore: utente non trovato nel DB"
            refreshUI(ref, store)
            return
        }

        val isAdmin = user.isAdmin
        val isOp = OpPermissionsUtil.isOp(player) == true
        if (!isAdmin && !isOp) {
            statusMessage = "Nessun permesso"
            refreshUI(ref, store)
            return
        }

        if (data.action == "tab") {
            activeTabId =
                when (data.tab.ifBlank { activeTabId }) {
                    TAB_LOGS -> TAB_LOGS
                    TAB_POSTS -> TAB_POSTS
                    TAB_MESSAGES -> TAB_MESSAGES
                    TAB_GUILDS -> TAB_GUILDS
                    else -> TAB_CONFIG
                }
            statusMessage = ""
        } else {
            // Delega gestione evento al tab corrente
            val resultMessage = tabs[activeTabId]?.handleEvent(data, user, isOp)
            if (resultMessage != null) {
                statusMessage = resultMessage
            }
        }

        refreshUI(ref, store)
    }

    private fun refreshUI(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
    ) {
        val cmd = UICommandBuilder()
        val evt = UIEventBuilder()
        this.build(ref, cmd, evt, store)
        this.sendUpdate(cmd, evt, true)
    }

    data class EventData(
        var action: String = "tab",
        var tab: String = "config",
        var id: Int = -1,
        var guildId: Int = -1,
        var userId: Int = -1,
        var postsRequiredItemIdToPost: String = "",
        var messagesRequiredItemIdToSend: String = "",
        var guildBoardRequiredItemIdToPost: String = "",
        var guildsRequiredItemIdToCreate: String = "",
        var logsMaxFileMB: String = "",
        var logsMaxFilesToKeep: String = "",
    ) {
        companion object {
            val CODEC: BuilderCodec<EventData> =
                BuilderCodec
                    .builder(EventData::class.java, ::EventData)
                    .append(KeyedCodec("Action", Codec.STRING), { e, v -> e.action = v }, { e -> e.action })
                    .add()
                    .append(KeyedCodec("Tab", Codec.STRING), { e, v -> e.tab = v }, { e -> e.tab })
                    .add()
                    .append(KeyedCodec("Id", Codec.STRING), { e, v -> e.id = v.toIntOrNull() ?: -1 }, { e -> e.id.toString() })
                    .add()
                    .append(
                        KeyedCodec("GuildId", Codec.STRING),
                        { e, v -> e.guildId = v.toIntOrNull() ?: -1 },
                        { e -> e.guildId.toString() },
                    ).add()
                    .append(
                        KeyedCodec("UserId", Codec.STRING),
                        { e, v -> e.userId = v.toIntOrNull() ?: -1 },
                        { e -> e.userId.toString() },
                    ).add()
                    .append(
                        KeyedCodec("PostsRequiredItemIdToPost", Codec.STRING),
                        { e, v -> e.postsRequiredItemIdToPost = v },
                        { e -> e.postsRequiredItemIdToPost },
                    ).add()
                    .append(
                        KeyedCodec("@PostsRequiredItemIdToPost", Codec.STRING),
                        { e, v -> e.postsRequiredItemIdToPost = v },
                        { e -> e.postsRequiredItemIdToPost },
                    ).add()
                    .append(
                        KeyedCodec("MessagesRequiredItemIdToSend", Codec.STRING),
                        { e, v -> e.messagesRequiredItemIdToSend = v },
                        { e -> e.messagesRequiredItemIdToSend },
                    ).add()
                    .append(
                        KeyedCodec("@MessagesRequiredItemIdToSend", Codec.STRING),
                        { e, v -> e.messagesRequiredItemIdToSend = v },
                        { e -> e.messagesRequiredItemIdToSend },
                    ).add()
                    .append(
                        KeyedCodec("GuildBoardRequiredItemIdToPost", Codec.STRING),
                        { e, v -> e.guildBoardRequiredItemIdToPost = v },
                        { e -> e.guildBoardRequiredItemIdToPost },
                    ).add()
                    .append(
                        KeyedCodec("@GuildBoardRequiredItemIdToPost", Codec.STRING),
                        { e, v -> e.guildBoardRequiredItemIdToPost = v },
                        { e -> e.guildBoardRequiredItemIdToPost },
                    ).add()
                    .append(
                        KeyedCodec("GuildsRequiredItemIdToCreate", Codec.STRING),
                        { e, v -> e.guildsRequiredItemIdToCreate = v },
                        { e -> e.guildsRequiredItemIdToCreate },
                    ).add()
                    .append(
                        KeyedCodec("@GuildsRequiredItemIdToCreate", Codec.STRING),
                        { e, v -> e.guildsRequiredItemIdToCreate = v },
                        { e -> e.guildsRequiredItemIdToCreate },
                    ).add()
                    .append(KeyedCodec("LogsMaxFileMB", Codec.STRING), { e, v -> e.logsMaxFileMB = v }, { e -> e.logsMaxFileMB })
                    .add()
                    .append(KeyedCodec("@LogsMaxFileMB", Codec.STRING), { e, v -> e.logsMaxFileMB = v }, { e -> e.logsMaxFileMB })
                    .add()
                    .append(
                        KeyedCodec("LogsMaxFilesToKeep", Codec.STRING),
                        { e, v -> e.logsMaxFilesToKeep = v },
                        { e -> e.logsMaxFilesToKeep },
                    ).add()
                    .append(
                        KeyedCodec("@LogsMaxFilesToKeep", Codec.STRING),
                        { e, v -> e.logsMaxFilesToKeep = v },
                        { e -> e.logsMaxFilesToKeep },
                    ).add()
                    .build()
        }
    }
}
