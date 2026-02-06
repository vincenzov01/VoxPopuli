package com.veim.voxpopuli.ui

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.ui.builder.EventData as UIEventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.VoxPopuliPlugin
import com.veim.voxpopuli.config.VoxPopuliConfig
import com.veim.voxpopuli.database.GuildServices
import com.veim.voxpopuli.database.GuildRank
import com.veim.voxpopuli.database.GuildMember
import com.veim.voxpopuli.database.MessageServices
import com.veim.voxpopuli.database.PostServices
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.util.OpPermissionsUtil
import java.util.logging.Level
import java.text.SimpleDateFormat
import java.util.Date

class VoxPopuliAdminDashboardPage(playerRef: PlayerRef) : InteractiveCustomUIPage<VoxPopuliAdminDashboardPage.EventData>(
    playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC
) {
    constructor(playerRef: PlayerRef, plugin: VoxPopuliPlugin) : this(playerRef) {
        this.plugin = plugin
        this.configSnapshot = plugin.config()
    }

    companion object {
        private val LOGGER = HytaleLogger.forEnclosingClass()
        private const val TAB_CONFIG: String = "config"
        private const val TAB_LOGS: String = "logs"
		private const val TAB_POSTS: String = "posts"
		private const val TAB_MESSAGES: String = "messages"
		private const val TAB_GUILDS: String = "guilds"
    }

    private lateinit var plugin: VoxPopuliPlugin
    private var configSnapshot: VoxPopuliConfig = VoxPopuliConfig()

    private val player: PlayerRef = playerRef

    private var activeTabId: String = TAB_CONFIG
    private var statusMessage: String = ""

    private var postsStatus: String = ""
    private var messagesStatus: String = ""
    private var guildBoardStatus: String = ""
	private var guildsStatus: String = ""
	private var selectedGuildId: Int = -1

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"
    private fun enableDisable(value: Boolean): String = if (value) "Disabilita" else "Abilita"

    private fun uiEventData(
        action: String,
        tab: String? = null,
        id: Int? = null,
		guildId: Int? = null,
		userId: Int? = null,
        captures: Map<String, String> = emptyMap(),
    ): UIEventData {
        // Hytale KeyedCodec keys must start with an uppercase character.
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

    private fun bindEvents(evt: UIEventBuilder) {
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabConfig",
            uiEventData(action = "tab", tab = TAB_CONFIG),
            false
        )
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabLogs",
            uiEventData(action = "tab", tab = TAB_LOGS),
            false
        )
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabPosts",
            uiEventData(action = "tab", tab = TAB_POSTS),
            false
        )
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabMessages",
            uiEventData(action = "tab", tab = TAB_MESSAGES),
            false
        )
        // Removed binding for TAB_GUILD_BOARD
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			"#TabGuilds",
			uiEventData(action = "tab", tab = TAB_GUILDS),
			false
		)

        // Tabs
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleTabsCronacheButton", uiEventData("toggle_tabs_cronache"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleTabsMissiveButton", uiEventData("toggle_tabs_missive"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleTabsGildaButton", uiEventData("toggle_tabs_gilda"), false)

        // Missive
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleMessagesAllowDeleteButton", uiEventData("toggle_messages_allow_delete"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleMessagesRequireItemToSendButton", uiEventData("toggle_messages_require_item_to_send"), false)

        // Posts
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#TogglePostsRequireItemToPostButton", uiEventData("toggle_posts_require_item_to_post"), false)

        // Bacheca
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleGuildBoardAllowPostButton", uiEventData("toggle_guildboard_allow_post"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleGuildBoardRequireItemToPostButton", uiEventData("toggle_guildboard_require_item_to_post"), false)

        // Gilde
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleGuildsRequireItemToCreateButton", uiEventData("toggle_guilds_require_item_to_create"), false)

        // Save / Reload
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SaveConfigButton",
            uiEventData(
                action = "save_config",
                captures = mapOf(
                    "PostsRequiredItemIdToPost" to "#PostsRequiredItemIdToPostInput.Value",
                    "MessagesRequiredItemIdToSend" to "#MessagesRequiredItemIdToSendInput.Value",
                    "GuildBoardRequiredItemIdToPost" to "#GuildBoardRequiredItemIdToPostInput.Value",
                    "GuildsRequiredItemIdToCreate" to "#GuildsRequiredItemIdToCreateInput.Value",
                )
            ),
            false
        )
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadConfigButton", uiEventData("reload_config"), false)

		// Moderation refresh
		evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshPostsButton", uiEventData("refresh_posts"), false)
		evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshMessagesButton", uiEventData("refresh_messages"), false)
		evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshGuildBoardButton", uiEventData("refresh_guild_board"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshGuildsButton", uiEventData("refresh_guilds"), false)
    }

    private fun sortedMembersForAdmin(members: List<GuildMember>): List<GuildMember> {
        return members.sortedWith(
            compareBy<GuildMember>({
                when (it.rank) {
                    GuildRank.OWNER -> 0
                    GuildRank.OFFICER -> 1
                    GuildRank.MEMBER -> 2
                }
            }).thenBy { it.userId }
        )
    }

    private fun renderGuildMembers(cmd: UICommandBuilder, evt: UIEventBuilder, guildId: Int) {
        cmd.clear("#AdminGuildMembersList")
        val guild = GuildServices.getGuildById(guildId)
        if (guild == null) {
            cmd.set("#SelectedGuildLabel.Text", "Membri gilda: (nessuna)")
            return
        }

        cmd.set("#SelectedGuildLabel.Text", "Membri gilda: ${guild.name}")

        val members = sortedMembersForAdmin(guild.members)
        members.forEachIndexed { index, member ->
            cmd.append("#AdminGuildMembersList", "voxpopuli/AdminGuildMemberItem.ui")
            val selector = "#AdminGuildMembersList[${index}]"
            val username = UserServices.getUserById(member.userId)?.username ?: "?"
            cmd.set("$selector #MemberName.Text", username)
            cmd.set("$selector #MemberRank.Text", member.rank.name)

            val isOwner = member.rank == GuildRank.OWNER || member.userId == guild.ownerId
            cmd.set("$selector #OwnerButton.Visible", !isOwner)
            cmd.set("$selector #KickButton.Visible", !isOwner)
            cmd.set("$selector #PromoteButton.Visible", member.rank == GuildRank.MEMBER)
            cmd.set("$selector #DemoteButton.Visible", member.rank == GuildRank.OFFICER)

            if (!isOwner) {
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #OwnerButton",
                    uiEventData(action = "set_owner_admin", tab = TAB_GUILDS, guildId = guild.id, userId = member.userId),
                    false
                )
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #KickButton",
                    uiEventData(action = "kick_member_admin", tab = TAB_GUILDS, guildId = guild.id, userId = member.userId),
                    false
                )
            }

            if (member.rank == GuildRank.MEMBER) {
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #PromoteButton",
                    uiEventData(action = "promote_member_admin", tab = TAB_GUILDS, guildId = guild.id, userId = member.userId),
                    false
                )
            }
            if (member.rank == GuildRank.OFFICER) {
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #DemoteButton",
                    uiEventData(action = "demote_member_admin", tab = TAB_GUILDS, guildId = guild.id, userId = member.userId),
                    false
                )
            }
        }
    }

    private fun renderGuilds(cmd: UICommandBuilder, evt: UIEventBuilder) {
        cmd.clear("#AdminGuildList")
        val guilds = GuildServices.getAllGuilds(limit = 200)
        guildsStatus = "${guilds.size} gilde"
        cmd.set("#GuildsStatusLabel.Text", guildsStatus)

        guilds.forEachIndexed { index, guild ->
            cmd.append("#AdminGuildList", "voxpopuli/AdminGuildItem.ui")
            val selector = "#AdminGuildList[${index}]"
            val ownerName = UserServices.getUserById(guild.ownerId)?.username ?: "?"
            cmd.set("$selector #GuildName.Text", guild.name)
            cmd.set("$selector #GuildMeta.Text", "Owner: $ownerName | Membri: ${guild.members.size}")

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #OpenButton",
                uiEventData(action = "open_guild", tab = TAB_GUILDS, guildId = guild.id),
                false
            )
            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                uiEventData(action = "delete_guild_admin", tab = TAB_GUILDS, guildId = guild.id),
                false
            )
        }

        if (selectedGuildId > 0) {
            renderGuildMembers(cmd, evt, selectedGuildId)
        } else {
            cmd.clear("#AdminGuildMembersList")
            cmd.set("#SelectedGuildLabel.Text", "Membri gilda: (nessuna)")
        }
    }

    private fun renderPosts(cmd: UICommandBuilder, evt: UIEventBuilder) {
        cmd.clear("#AdminPostList")
        val posts = PostServices.getAllPosts().sortedByDescending { it.timestamp }
		postsStatus = "${posts.size} post"

        posts.forEachIndexed { index, post ->
            cmd.append("#AdminPostList", "voxpopuli/AdminPostItem.ui")
            val selector = "#AdminPostList[${index}]"
            val author = UserServices.getUserById(post.authorId)?.username ?: "?"
            cmd.set("$selector #PostAuthor.Text", author)
            cmd.set("$selector #PostTimestamp.Text", dateFormat.format(Date(post.timestamp)))
            cmd.set("$selector #PostContent.Text", post.content)
            cmd.set("$selector #LikeCount.Text", "${post.likedBy.size} like")

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                uiEventData(action = "delete_post", id = post.id),
                false
            )
        }
        cmd.set("#PostsStatusLabel.Text", postsStatus)
    }

    private fun renderMessages(cmd: UICommandBuilder, evt: UIEventBuilder) {
        cmd.clear("#AdminMessageList")
        val messages = MessageServices.getAllMessages(limit = 200)
		messagesStatus = "${messages.size} messaggi"

        messages.forEachIndexed { index, message ->
            cmd.append("#AdminMessageList", "voxpopuli/AdminMessageItem.ui")
            val selector = "#AdminMessageList[${index}]"
            val sender = UserServices.getUserById(message.senderId)?.username ?: "?"
            val receiver = UserServices.getUserById(message.receiverId)?.username ?: "?"
            cmd.set("$selector #MessageSender.Text", "$sender -> $receiver")
            cmd.set("$selector #MessageTimestamp.Text", dateFormat.format(Date(message.timestamp)))
            cmd.set("$selector #MessageContent.Text", message.content)

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                uiEventData(action = "delete_message_admin", id = message.id),
                false
            )
        }
        cmd.set("#MessagesStatusLabel.Text", messagesStatus)
    }

    private fun renderGuildBoard(cmd: UICommandBuilder, evt: UIEventBuilder) {
        cmd.clear("#AdminGuildBoardList")
        val guildId = selectedGuildId
		if (guildId <= 0) {
            guildBoardStatus = "Seleziona una gilda nella tab Gilde"
            cmd.set("#GuildBoardStatusLabel.Text", guildBoardStatus)
            return
        }

        val guild = GuildServices.getGuildById(guildId)
        val board = GuildServices.getBoardMessages(guildId).take(200)
        val guildName = guild?.name ?: "Gilda #$guildId"
        guildBoardStatus = "$guildName: ${board.size} comunicazioni"

        board.forEachIndexed { index, message ->
            cmd.append("#AdminGuildBoardList", "voxpopuli/AdminGuildBoardItem.ui")
            val selector = "#AdminGuildBoardList[${index}]"
            val author = UserServices.getUserById(message.authorId)?.username ?: "?"
            cmd.set("$selector #GuildName.Text", guildName)
            cmd.set("$selector #BoardAuthor.Text", author)
            cmd.set("$selector #BoardTimestamp.Text", dateFormat.format(Date(message.timestamp)))
            cmd.set("$selector #BoardContent.Text", message.content)

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                uiEventData(action = "delete_guild_board_admin", id = message.id),
                false
            )
        }

        cmd.set("#GuildBoardStatusLabel.Text", guildBoardStatus)
    }

    private fun render(cmd: UICommandBuilder) {
        cmd.set("#Username.Text", player.username)
        cmd.set("#RoleRole.Text", "Admin")
        cmd.set("#ContentTitle.Text", "VoxPopuli Admin")

        // Tabs container visibility
        cmd.set("#ConfigContent.Visible", activeTabId == TAB_CONFIG)
        cmd.set("#LogsContent.Visible", activeTabId == TAB_LOGS)
		cmd.set("#PostsContent.Visible", activeTabId == TAB_POSTS)
		cmd.set("#MessagesContent.Visible", activeTabId == TAB_MESSAGES)
		// cmd.set("#GuildBoardContent.Visible", activeTabId == TAB_GUILD_BOARD) // Removed visibility toggle
        cmd.set("#GuildsContent.Visible", activeTabId == TAB_GUILDS)

        // Tabs toggles
        cmd.set("#TabsCronacheValue.Text", onOff(configSnapshot.tabs.enableCronache))
        cmd.set("#ToggleTabsCronacheButton.Text", enableDisable(configSnapshot.tabs.enableCronache))

        cmd.set("#TabsMissiveValue.Text", onOff(configSnapshot.tabs.enableMissive))
        cmd.set("#ToggleTabsMissiveButton.Text", enableDisable(configSnapshot.tabs.enableMissive))

        cmd.set("#TabsGildaValue.Text", onOff(configSnapshot.tabs.enableGilda))
        cmd.set("#ToggleTabsGildaButton.Text", enableDisable(configSnapshot.tabs.enableGilda))

        // Posts
        cmd.set("#PostsRequireItemToPostValue.Text", onOff(configSnapshot.posts.requireItemToPost))
        cmd.set("#TogglePostsRequireItemToPostButton.Text", enableDisable(configSnapshot.posts.requireItemToPost))
        cmd.set("#PostsRequiredItemIdToPostRow.Visible", configSnapshot.posts.requireItemToPost)
        cmd.set("#PostsRequiredItemIdToPostInput.Value", configSnapshot.posts.requiredItemIdToPost)

        // Missive
        cmd.set("#MessagesAllowDeleteValue.Text", onOff(configSnapshot.messages.allowDelete))
        cmd.set("#ToggleMessagesAllowDeleteButton.Text", enableDisable(configSnapshot.messages.allowDelete))

        cmd.set("#MessagesRequireItemToSendValue.Text", onOff(configSnapshot.messages.requireItemToSend))
        cmd.set("#ToggleMessagesRequireItemToSendButton.Text", enableDisable(configSnapshot.messages.requireItemToSend))
        cmd.set("#MessagesRequiredItemIdToSendRow.Visible", configSnapshot.messages.requireItemToSend)
        cmd.set("#MessagesRequiredItemIdToSendInput.Value", configSnapshot.messages.requiredItemIdToSend)

        // Bacheca gilda
        cmd.set("#GuildBoardAllowPostValue.Text", onOff(configSnapshot.guildBoard.allowPost))
        cmd.set("#ToggleGuildBoardAllowPostButton.Text", enableDisable(configSnapshot.guildBoard.allowPost))

        cmd.set("#GuildBoardRequireItemToPostValue.Text", onOff(configSnapshot.guildBoard.requireItemToPost))
        cmd.set("#ToggleGuildBoardRequireItemToPostButton.Text", enableDisable(configSnapshot.guildBoard.requireItemToPost))
        cmd.set("#GuildBoardRequiredItemIdToPostRow.Visible", configSnapshot.guildBoard.requireItemToPost)
        cmd.set("#GuildBoardRequiredItemIdToPostInput.Value", configSnapshot.guildBoard.requiredItemIdToPost)

        // Gilde
        cmd.set("#GuildsRequireItemToCreateValue.Text", onOff(configSnapshot.guilds.requireItemToCreate))
        cmd.set("#ToggleGuildsRequireItemToCreateButton.Text", enableDisable(configSnapshot.guilds.requireItemToCreate))
        cmd.set("#GuildsRequiredItemIdToCreateRow.Visible", configSnapshot.guilds.requireItemToCreate)
        cmd.set("#GuildsRequiredItemIdToCreateInput.Value", configSnapshot.guilds.requiredItemIdToCreate)

        cmd.set("#SaveStatusLabel.Text", statusMessage)
        cmd.set("#PostsStatusLabel.Text", postsStatus)
        cmd.set("#MessagesStatusLabel.Text", messagesStatus)
        cmd.set("#GuildBoardStatusLabel.Text", guildBoardStatus)
		cmd.set("#GuildsStatusLabel.Text", guildsStatus)
		cmd.set("#SelectedGuildLabel.Text", if (selectedGuildId > 0) "Membri gilda: #$selectedGuildId" else "Membri gilda: (nessuna)")
    }

    override fun build(
        ref: Ref<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
        store: Store<EntityStore>
    ) {
        buildLayout(cmd)
        bindEvents(evt)
        render(cmd)
    }

    override fun handleDataEvent(ref: Ref<EntityStore>, store: Store<EntityStore>, data: EventData) {
        LOGGER.at(Level.INFO).log(
			"[VoxPopuliAdmin] UI event: action=%s tab=%s id=%s guildId=%s userId=%s msgsItem='%s' boardItem='%s' guildCreateItem='%s'",
            data.action,
            data.tab,
			data.id,
			data.guildId,
			data.userId,
            data.messagesRequiredItemIdToSend,
            data.guildBoardRequiredItemIdToPost,
            data.guildsRequiredItemIdToCreate
        )

        val user = UserServices.getUserByUsername(player.username) ?: UserServices.createUser(player.username)
        if (user == null) {
            statusMessage = "Errore: utente non trovato nel DB"
            val cmd = UICommandBuilder()
            val evt = UIEventBuilder()
            buildLayout(cmd)
            bindEvents(evt)
            render(cmd)
            this.sendUpdate(cmd, evt, true)
            return
        }
        val isAdmin = user.isAdmin
		val isOp = OpPermissionsUtil.isOp(player) == true
		if (!isAdmin && !isOp) {
            statusMessage = "Nessun permesso"
            val cmd = UICommandBuilder()
            val evt = UIEventBuilder()
            buildLayout(cmd)
            bindEvents(evt)
            render(cmd)
            this.sendUpdate(cmd, evt, true)
            return
        }

        when (data.action) {
            "tab" -> {
                activeTabId = when (data.tab.ifBlank { activeTabId }) {
                    TAB_LOGS -> TAB_LOGS
                    TAB_POSTS -> TAB_POSTS
                    TAB_MESSAGES -> TAB_MESSAGES
					TAB_GUILDS -> TAB_GUILDS
                    else -> TAB_CONFIG
                }
				statusMessage = ""
            }

            "toggle_tabs_cronache" -> {
                configSnapshot.tabs.enableCronache = !configSnapshot.tabs.enableCronache
                statusMessage = "Modifiche non salvate"
            }

            "toggle_tabs_missive" -> {
                configSnapshot.tabs.enableMissive = !configSnapshot.tabs.enableMissive
                statusMessage = "Modifiche non salvate"
            }

            "toggle_tabs_gilda" -> {
                configSnapshot.tabs.enableGilda = !configSnapshot.tabs.enableGilda
                statusMessage = "Modifiche non salvate"
            }

            "toggle_messages_allow_delete" -> {
                configSnapshot.messages.allowDelete = !configSnapshot.messages.allowDelete
                statusMessage = "Modifiche non salvate"
            }

            "toggle_messages_require_item_to_send" -> {
                configSnapshot.messages.requireItemToSend = !configSnapshot.messages.requireItemToSend
                statusMessage = "Modifiche non salvate"
            }

            "toggle_posts_require_item_to_post" -> {
                configSnapshot.posts.requireItemToPost = !configSnapshot.posts.requireItemToPost
                statusMessage = "Modifiche non salvate"
            }

            "toggle_guildboard_allow_post" -> {
                configSnapshot.guildBoard.allowPost = !configSnapshot.guildBoard.allowPost
                statusMessage = "Modifiche non salvate"
            }

            "toggle_guildboard_require_item_to_post" -> {
                configSnapshot.guildBoard.requireItemToPost = !configSnapshot.guildBoard.requireItemToPost
                statusMessage = "Modifiche non salvate"
            }

            "toggle_guilds_require_item_to_create" -> {
                configSnapshot.guilds.requireItemToCreate = !configSnapshot.guilds.requireItemToCreate
                statusMessage = "Modifiche non salvate"
            }

            "reload_config" -> {
                configSnapshot = plugin.config()
                statusMessage = "Ricaricato dal DB"
            }

            "save_config" -> {
                configSnapshot.posts.requiredItemIdToPost = data.postsRequiredItemIdToPost.trim()
                configSnapshot.messages.requiredItemIdToSend = data.messagesRequiredItemIdToSend.trim()
                configSnapshot.guildBoard.requiredItemIdToPost = data.guildBoardRequiredItemIdToPost.trim()
                configSnapshot.guilds.requiredItemIdToCreate = data.guildsRequiredItemIdToCreate.trim()

                plugin.saveConfig(configSnapshot)
                configSnapshot = plugin.config()
                statusMessage = "Salvato nel DB"
            }

            "refresh_posts" -> {
                postsStatus = "Aggiornato"
            }
            "refresh_messages" -> {
                messagesStatus = "Aggiornato"
            }
            "refresh_guild_board" -> {
                guildBoardStatus = "Aggiornato"
            }
            "refresh_guilds" -> {
                guildsStatus = "Aggiornato"
            }

            "open_guild" -> {
                val gid = data.guildId
                if (gid > 0) {
                    selectedGuildId = gid
                    guildsStatus = "Gilda aperta"
                }
            }
            "delete_guild_admin" -> {
                val gid = data.guildId
                if (gid > 0) {
                    GuildServices.deleteGuild(gid)
                    if (selectedGuildId == gid) selectedGuildId = -1
                    guildsStatus = "Gilda eliminata"
                }
            }
            "kick_member_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    GuildServices.removeMember(gid, uid)
                    guildsStatus = "Membro rimosso"
                }
            }
            "promote_member_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    val guild = GuildServices.getGuildById(gid)
                    val rank = guild?.members?.firstOrNull { it.userId == uid }?.rank
                    if (rank == GuildRank.MEMBER) {
                        GuildServices.setRank(gid, uid, GuildRank.OFFICER)
                        guildsStatus = "Promosso"
                    }
                }
            }
            "demote_member_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    val guild = GuildServices.getGuildById(gid)
                    val rank = guild?.members?.firstOrNull { it.userId == uid }?.rank
                    if (rank == GuildRank.OFFICER) {
                        GuildServices.setRank(gid, uid, GuildRank.MEMBER)
                        guildsStatus = "Retrocesso"
                    }
                }
            }
            "set_owner_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    GuildServices.transferOwnership(gid, uid)
                    selectedGuildId = gid
                    guildsStatus = "Owner cambiato"
                }
            }

            "delete_post" -> {
                val id = data.id
                if (id > 0) {
                    PostServices.deletePost(id)
                    postsStatus = "Post eliminato"
                }
            }
            "delete_message_admin" -> {
                val id = data.id
                if (id > 0) {
                    MessageServices.deleteMessage(id, userId = user.id, allowAdminBypass = true)
                    messagesStatus = "Messaggio eliminato"
                }
            }
            "delete_guild_board_admin" -> {
                val id = data.id
                if (id > 0) {
                    GuildServices.deleteBoardMessage(id, userId = user.id, allowAdminBypass = true)
                    guildBoardStatus = "Comunicazione eliminata"
                }
            }
        }

        val cmd = UICommandBuilder()
        val evt = UIEventBuilder()
        buildLayout(cmd)
        bindEvents(evt)
        render(cmd)

        // Render lists only when their tab is active (keeps updates cheap)
        when (activeTabId) {
            TAB_POSTS -> renderPosts(cmd, evt)
            TAB_MESSAGES -> renderMessages(cmd, evt)
			TAB_GUILDS -> {
				renderGuilds(cmd, evt)
				renderGuildBoard(cmd, evt)
			}
        }

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
    ) {
        companion object {
            val CODEC: BuilderCodec<EventData> = BuilderCodec.builder(EventData::class.java, ::EventData)
                .append(KeyedCodec("Action", Codec.STRING), { e, v -> e.action = v }, { e -> e.action }).add()
                .append(KeyedCodec("Tab", Codec.STRING), { e, v -> e.tab = v }, { e -> e.tab }).add()
				.append(KeyedCodec("Id", Codec.STRING), { e, v -> e.id = v.toIntOrNull() ?: -1 }, { e -> e.id.toString() }).add()
				.append(KeyedCodec("GuildId", Codec.STRING), { e, v -> e.guildId = v.toIntOrNull() ?: -1 }, { e -> e.guildId.toString() }).add()
				.append(KeyedCodec("UserId", Codec.STRING), { e, v -> e.userId = v.toIntOrNull() ?: -1 }, { e -> e.userId.toString() }).add()

                // Captures from Save button
                .append(KeyedCodec("PostsRequiredItemIdToPost", Codec.STRING), { e, v -> e.postsRequiredItemIdToPost = v }, { e -> e.postsRequiredItemIdToPost }).add()
                .append(KeyedCodec("@PostsRequiredItemIdToPost", Codec.STRING), { e, v -> e.postsRequiredItemIdToPost = v }, { e -> e.postsRequiredItemIdToPost }).add()
                .append(KeyedCodec("MessagesRequiredItemIdToSend", Codec.STRING), { e, v -> e.messagesRequiredItemIdToSend = v }, { e -> e.messagesRequiredItemIdToSend }).add()
                .append(KeyedCodec("@MessagesRequiredItemIdToSend", Codec.STRING), { e, v -> e.messagesRequiredItemIdToSend = v }, { e -> e.messagesRequiredItemIdToSend }).add()

                .append(KeyedCodec("GuildBoardRequiredItemIdToPost", Codec.STRING), { e, v -> e.guildBoardRequiredItemIdToPost = v }, { e -> e.guildBoardRequiredItemIdToPost }).add()
                .append(KeyedCodec("@GuildBoardRequiredItemIdToPost", Codec.STRING), { e, v -> e.guildBoardRequiredItemIdToPost = v }, { e -> e.guildBoardRequiredItemIdToPost }).add()

                .append(KeyedCodec("GuildsRequiredItemIdToCreate", Codec.STRING), { e, v -> e.guildsRequiredItemIdToCreate = v }, { e -> e.guildsRequiredItemIdToCreate }).add()
                .append(KeyedCodec("@GuildsRequiredItemIdToCreate", Codec.STRING), { e, v -> e.guildsRequiredItemIdToCreate = v }, { e -> e.guildsRequiredItemIdToCreate }).add()

                .build()
        }
    }
}
