package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.VoxPopuliPlugin
import com.veim.voxpopuli.config.VoxPopuliConfig
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.FileAuditLog

class ConfigTab(
    private val page: VoxPopuliAdminDashboardPage,
    private val plugin: VoxPopuliPlugin,
) : AdminTab {
    override val tabId: String = VoxPopuliAdminDashboardPage.TAB_CONFIG
    private var configSnapshot: VoxPopuliConfig = plugin.config()

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

    private fun enableDisable(value: Boolean): String = if (value) "Disabilita" else "Abilita"

    override fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        // Tabs toggles
        cmd.set("#TabsCronacheValue.Text", onOff(configSnapshot.tabs.enableCronache))
        cmd.set("#ToggleTabsCronacheButton.Text", enableDisable(configSnapshot.tabs.enableCronache))
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleTabsCronacheButton",
            page.uiEventData("toggle_tabs_cronache"),
            false,
        )

        cmd.set("#TabsMissiveValue.Text", onOff(configSnapshot.tabs.enableMissive))
        cmd.set("#ToggleTabsMissiveButton.Text", enableDisable(configSnapshot.tabs.enableMissive))
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleTabsMissiveButton", page.uiEventData("toggle_tabs_missive"), false)

        cmd.set("#TabsGildaValue.Text", onOff(configSnapshot.tabs.enableGilda))
        cmd.set("#ToggleTabsGildaButton.Text", enableDisable(configSnapshot.tabs.enableGilda))
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleTabsGildaButton", page.uiEventData("toggle_tabs_gilda"), false)

        // Posts
        cmd.set("#PostsRequireItemToPostValue.Text", onOff(configSnapshot.posts.requireItemToPost))
        cmd.set("#TogglePostsRequireItemToPostButton.Text", enableDisable(configSnapshot.posts.requireItemToPost))
        cmd.set("#PostsRequiredItemIdToPostRow.Visible", configSnapshot.posts.requireItemToPost)
        cmd.set("#PostsRequiredItemIdToPostInput.Value", configSnapshot.posts.requiredItemIdToPost)
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TogglePostsRequireItemToPostButton",
            page.uiEventData("toggle_posts_require_item_to_post"),
            false,
        )

        // Missive
        cmd.set("#MessagesAllowDeleteValue.Text", onOff(configSnapshot.messages.allowDelete))
        cmd.set("#ToggleMessagesAllowDeleteButton.Text", enableDisable(configSnapshot.messages.allowDelete))
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleMessagesAllowDeleteButton",
            page.uiEventData("toggle_messages_allow_delete"),
            false,
        )

        cmd.set("#MessagesRequireItemToSendValue.Text", onOff(configSnapshot.messages.requireItemToSend))
        cmd.set("#ToggleMessagesRequireItemToSendButton.Text", enableDisable(configSnapshot.messages.requireItemToSend))
        cmd.set("#MessagesRequiredItemIdToSendRow.Visible", configSnapshot.messages.requireItemToSend)
        cmd.set("#MessagesRequiredItemIdToSendInput.Value", configSnapshot.messages.requiredItemIdToSend)
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleMessagesRequireItemToSendButton",
            page.uiEventData("toggle_messages_require_item_to_send"),
            false,
        )

        // Bacheca gilda
        cmd.set("#GuildBoardAllowPostValue.Text", onOff(configSnapshot.guildBoard.allowPost))
        cmd.set("#ToggleGuildBoardAllowPostButton.Text", enableDisable(configSnapshot.guildBoard.allowPost))
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleGuildBoardAllowPostButton",
            page.uiEventData("toggle_guildboard_allow_post"),
            false,
        )

        cmd.set("#GuildBoardRequireItemToPostValue.Text", onOff(configSnapshot.guildBoard.requireItemToPost))
        cmd.set("#ToggleGuildBoardRequireItemToPostButton.Text", enableDisable(configSnapshot.guildBoard.requireItemToPost))
        cmd.set("#GuildBoardRequiredItemIdToPostRow.Visible", configSnapshot.guildBoard.requireItemToPost)
        cmd.set("#GuildBoardRequiredItemIdToPostInput.Value", configSnapshot.guildBoard.requiredItemIdToPost)
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleGuildBoardRequireItemToPostButton",
            page.uiEventData("toggle_guildboard_require_item_to_post"),
            false,
        )

        // Gilde
        cmd.set("#GuildsRequireItemToCreateValue.Text", onOff(configSnapshot.guilds.requireItemToCreate))
        cmd.set("#ToggleGuildsRequireItemToCreateButton.Text", enableDisable(configSnapshot.guilds.requireItemToCreate))
        cmd.set("#GuildsRequiredItemIdToCreateRow.Visible", configSnapshot.guilds.requireItemToCreate)
        cmd.set("#GuildsRequiredItemIdToCreateInput.Value", configSnapshot.guilds.requiredItemIdToCreate)
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleGuildsRequireItemToCreateButton",
            page.uiEventData("toggle_guilds_require_item_to_create"),
            false,
        )

        // Config logs (valori attuali nei campi input)
        cmd.set("#LogsMaxFileMBInput.Value", configSnapshot.logs.maxFileMB.toString())
        cmd.set("#LogsMaxFilesToKeepInput.Value", configSnapshot.logs.maxFilesToKeep.toString())

        // Save / Reload
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SaveConfigButton",
            page.uiEventData(
                action = "save_config",
                captures =
                    mapOf(
                        "PostsRequiredItemIdToPost" to "#PostsRequiredItemIdToPostInput.Value",
                        "MessagesRequiredItemIdToSend" to "#MessagesRequiredItemIdToSendInput.Value",
                        "GuildBoardRequiredItemIdToPost" to "#GuildBoardRequiredItemIdToPostInput.Value",
                        "GuildsRequiredItemIdToCreate" to "#GuildsRequiredItemIdToCreateInput.Value",
                        "LogsMaxFileMB" to "#LogsMaxFileMBInput.Value",
                        "LogsMaxFilesToKeep" to "#LogsMaxFilesToKeepInput.Value",
                    ),
            ),
            false,
        )
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadConfigButton", page.uiEventData("reload_config"), false)
    }

    override fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String? =
        when (data.action) {
            "toggle_tabs_cronache" -> {
                configSnapshot.tabs.enableCronache = !configSnapshot.tabs.enableCronache
                "Modifiche non salvate"
            }

            "toggle_tabs_missive" -> {
                configSnapshot.tabs.enableMissive = !configSnapshot.tabs.enableMissive
                "Modifiche non salvate"
            }

            "toggle_tabs_gilda" -> {
                configSnapshot.tabs.enableGilda = !configSnapshot.tabs.enableGilda
                "Modifiche non salvate"
            }

            "toggle_messages_allow_delete" -> {
                configSnapshot.messages.allowDelete = !configSnapshot.messages.allowDelete
                "Modifiche non salvate"
            }

            "toggle_messages_require_item_to_send" -> {
                configSnapshot.messages.requireItemToSend = !configSnapshot.messages.requireItemToSend
                "Modifiche non salvate"
            }

            "toggle_posts_require_item_to_post" -> {
                configSnapshot.posts.requireItemToPost = !configSnapshot.posts.requireItemToPost
                "Modifiche non salvate"
            }

            "toggle_guildboard_allow_post" -> {
                configSnapshot.guildBoard.allowPost = !configSnapshot.guildBoard.allowPost
                "Modifiche non salvate"
            }

            "toggle_guildboard_require_item_to_post" -> {
                configSnapshot.guildBoard.requireItemToPost = !configSnapshot.guildBoard.requireItemToPost
                "Modifiche non salvate"
            }

            "toggle_guilds_require_item_to_create" -> {
                configSnapshot.guilds.requireItemToCreate = !configSnapshot.guilds.requireItemToCreate
                "Modifiche non salvate"
            }

            "reload_config" -> {
                configSnapshot = plugin.config()
                "Ricaricato dal DB"
            }

            "save_config" -> {
                configSnapshot.posts.requiredItemIdToPost = data.postsRequiredItemIdToPost.trim()
                configSnapshot.messages.requiredItemIdToSend = data.messagesRequiredItemIdToSend.trim()
                configSnapshot.guildBoard.requiredItemIdToPost = data.guildBoardRequiredItemIdToPost.trim()
                configSnapshot.guilds.requiredItemIdToCreate = data.guildsRequiredItemIdToCreate.trim()

                // LOGS: aggiorna i valori dalla UI
                val maxFileMB =
                    data.logsMaxFileMB
                        .trim()
                        .toIntOrNull()
                        ?.coerceIn(1, 1000) ?: 5
                val maxFilesToKeep =
                    data.logsMaxFilesToKeep
                        .trim()
                        .toIntOrNull()
                        ?.coerceIn(1, 1000) ?: 50
                configSnapshot.logs.maxFileMB = maxFileMB
                configSnapshot.logs.maxFilesToKeep = maxFilesToKeep

                plugin.saveConfig(configSnapshot)
                configSnapshot = plugin.config()

                FileAuditLog.logAdminAction(
                    actorUsername = page.player.username,
                    actorUserId = user.id,
                    action = "config.save",
                    details =
                        mapOf(
                            "tabs.enableCronache" to configSnapshot.tabs.enableCronache.toString(),
                            "tabs.enableMissive" to configSnapshot.tabs.enableMissive.toString(),
                            // ... (per brevità puoi omettere dettagli ridondanti, o includere tutto come nell'originale)
                            "logs.maxFileMB" to configSnapshot.logs.maxFileMB.toString(),
                        ),
                )
                "Salvato nel DB"
            }

            else -> {
                null
            }
        }
}
