package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.database.GuildRank
import com.veim.voxpopuli.database.GuildServices
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage
import com.veim.voxpopuli.util.InventoryUtil
import java.text.SimpleDateFormat
import java.util.Date

object GildaTab : BaseVoxTab(id = "gilda", title = "Gilda") {
	override val templatePath: String = "voxpopuli/gilda.ui"

	override fun bindEvents(page: VoxPopuliDashboardPage, evt: UIEventBuilder) {
		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#CreateGuildNameInput"),
			page.uiEventData(action = "draft_create_guild_name"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#CreateGuildNameInput"),
			page.uiEventData(action = "draft_create_guild_name"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#CreateGuildNameInput"),
			page.uiEventData(action = "draft_create_guild_name"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#CreateGuildButton"),
			page.uiEventData(
				action = "create_guild",
				captures = mapOf(
					"GuildName" to page.tabSel("#CreateGuildNameInput.Value"),
				),
			),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#InviteMemberInput"),
			page.uiEventData(action = "draft_invite_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#InviteMemberInput"),
			page.uiEventData(action = "draft_invite_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#InviteMemberInput"),
			page.uiEventData(action = "draft_invite_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#InviteMemberButton"),
			page.uiEventData(
				action = "invite_member",
				captures = mapOf(
					"MemberName" to page.tabSel("#InviteMemberInput.Value"),
				),
			),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#PromoteMemberInput"),
			page.uiEventData(action = "draft_promote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#PromoteMemberInput"),
			page.uiEventData(action = "draft_promote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#PromoteMemberInput"),
			page.uiEventData(action = "draft_promote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#PromoteMemberButton"),
			page.uiEventData(
				action = "promote_member",
				captures = mapOf(
					"MemberName" to page.tabSel("#PromoteMemberInput.Value"),
				),
			),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#DemoteMemberInput"),
			page.uiEventData(action = "draft_demote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#DemoteMemberInput"),
			page.uiEventData(action = "draft_demote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#DemoteMemberInput"),
			page.uiEventData(action = "draft_demote_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#DemoteMemberButton"),
			page.uiEventData(
				action = "demote_member",
				captures = mapOf(
					"MemberName" to page.tabSel("#DemoteMemberInput.Value"),
				),
			),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#KickMemberInput"),
			page.uiEventData(action = "draft_kick_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#KickMemberInput"),
			page.uiEventData(action = "draft_kick_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#KickMemberInput"),
			page.uiEventData(action = "draft_kick_member"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#KickMemberButton"),
			page.uiEventData(
				action = "kick_member",
				captures = mapOf(
					"MemberName" to page.tabSel("#KickMemberInput.Value"),
				),
			),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#LeaveGuildButton"),
			page.uiEventData(action = "leave_guild"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#DeleteGuildButton"),
			page.uiEventData(action = "delete_guild"),
			false
		)

		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#GuildBoardInput"),
			page.uiEventData(action = "draft_guild_board"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#GuildBoardInput"),
			page.uiEventData(action = "draft_guild_board"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#GuildBoardInput"),
			page.uiEventData(action = "draft_guild_board"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#SendGuildBoardButton"),
			page.uiEventData(
				action = "send_guild_board",
				captures = mapOf(
					"BoardText" to page.tabSel("#GuildBoardInput.Value"),
				),
			),
			false
		)
	}

	override fun render(
		page: VoxPopuliDashboardPage,
		ref: Ref<EntityStore>,
		store: Store<EntityStore>,
		cmd: UICommandBuilder,
		evt: UIEventBuilder
	) {
		val boardConfig = page.configSnapshot.guildBoard
		cmd.set(page.tabSel("#GuildBoardInput.Visible"), boardConfig.allowPost)
		cmd.set(page.tabSel("#SendGuildBoardButton.Visible"), boardConfig.allowPost)

		// Keep inputs in sync
		cmd.set(page.tabSel("#CreateGuildNameInput.Value"), page.draftCreateGuildName)
		cmd.set(page.tabSel("#InviteMemberInput.Value"), page.draftInviteMemberUsername)
		cmd.set(page.tabSel("#PromoteMemberInput.Value"), page.draftPromoteMemberUsername)
		cmd.set(page.tabSel("#DemoteMemberInput.Value"), page.draftDemoteMemberUsername)
		cmd.set(page.tabSel("#KickMemberInput.Value"), page.draftKickMemberUsername)
		cmd.set(page.tabSel("#GuildBoardInput.Value"), page.draftGuildBoardText)

		cmd.clear(page.tabSel("#GuildMembersList"))
		cmd.clear(page.tabSel("#GuildBoardList"))

		val user = getOrCreateUser(page) ?: return
		val guild = user.guildId?.let { GuildServices.getGuildById(it) }
		cmd.set("#ContentTitle.Text", title)

		if (guild == null) {
			cmd.set(page.tabSel("#GuildName.Text"), "Nessuna gilda")
			cmd.set(page.tabSel("#GuildRole.Text"), "")

			cmd.set(page.tabSel("#GuildMembersSection.Visible"), false)
			cmd.set(page.tabSel("#GuildBoardSection.Visible"), false)

			cmd.set(page.tabSel("#CreateGuildBox.Visible"), true)
			cmd.set(page.tabSel("#InviteMemberBox.Visible"), false)
			cmd.set(page.tabSel("#PromoteMemberBox.Visible"), false)
			cmd.set(page.tabSel("#DemoteMemberBox.Visible"), false)
			cmd.set(page.tabSel("#KickMemberBox.Visible"), false)
			cmd.set(page.tabSel("#LeaveGuildBox.Visible"), false)
			cmd.set(page.tabSel("#DeleteGuildBox.Visible"), false)
			return
		}

		cmd.set(page.tabSel("#GuildMembersSection.Visible"), true)
		cmd.set(page.tabSel("#GuildBoardSection.Visible"), true)

		val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
		cmd.set(page.tabSel("#GuildName.Text"), guild.name)
		cmd.set(page.tabSel("#GuildRole.Text"), "Ruolo: ${selfRank.name}")

		val canManage = selfRank == GuildRank.OWNER || selfRank == GuildRank.OFFICER

		cmd.set(page.tabSel("#CreateGuildBox.Visible"), false)
		cmd.set(page.tabSel("#InviteMemberBox.Visible"), canManage)
		cmd.set(page.tabSel("#PromoteMemberBox.Visible"), canManage)
		cmd.set(page.tabSel("#DemoteMemberBox.Visible"), canManage)
		cmd.set(page.tabSel("#KickMemberBox.Visible"), canManage)
		cmd.set(page.tabSel("#LeaveGuildBox.Visible"), true)
		cmd.set(page.tabSel("#DeleteGuildBox.Visible"), selfRank == GuildRank.OWNER)

		val sortedMembers = guild.members.sortedWith(
			compareBy<com.veim.voxpopuli.database.GuildMember>({
				when (it.rank) {
					GuildRank.OWNER -> 0
					GuildRank.OFFICER -> 1
					GuildRank.MEMBER -> 2
				}
			}).thenBy { it.userId }
		)

		sortedMembers.forEachIndexed { index, member ->
			cmd.append(page.tabSel("#GuildMembersList"), "voxpopuli/GuildMemberItem.ui")
			val selector = page.tabSel("#GuildMembersList[${index}]")
			val name = UserServices.getUserById(member.userId)?.username ?: "?"
			cmd.set("$selector #MemberName.Text", name)
			cmd.set("$selector #MemberRank.Text", member.rank.name)
		}

		val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
		val board = GuildServices.getBoardMessages(guild.id)
		cmd.set("#ContentTitle.Text", "${title} (${sortedMembers.size} membri, ${board.size} bacheca)")
		board.forEachIndexed { index, message ->
			cmd.append(page.tabSel("#GuildBoardList"), "voxpopuli/GuildBoardItem.ui")
			val selector = page.tabSel("#GuildBoardList[${index}]")
			val author = UserServices.getUserById(message.authorId)?.username ?: "?"
			cmd.set("$selector #BoardAuthor.Text", author)
			cmd.set("$selector #BoardTimestamp.Text", dateFormat.format(Date(message.timestamp)))
			cmd.set("$selector #BoardContent.Text", message.content)
		}
	}

	override fun handleEvent(
		page: VoxPopuliDashboardPage,
		ref: Ref<EntityStore>,
		store: Store<EntityStore>,
		data: VoxPopuliDashboardPage.EventData,
		cmd: UICommandBuilder,
		evt: UIEventBuilder
	): Boolean {
		when (data.action) {
			"draft_create_guild_name" -> {
				page.draftCreateGuildName = data.text
				return true
			}
			"draft_invite_member" -> {
				page.draftInviteMemberUsername = data.text
				return true
			}
			"draft_promote_member" -> {
				page.draftPromoteMemberUsername = data.text
				return true
			}
			"draft_demote_member" -> {
				page.draftDemoteMemberUsername = data.text
				return true
			}
			"draft_kick_member" -> {
				page.draftKickMemberUsername = data.text
				return true
			}
			"draft_guild_board" -> {
				page.draftGuildBoardText = data.text
				return true
			}
			"create_guild" -> {
				val guildConfig = page.configSnapshot.guilds
				val user = getOrCreateUser(page) ?: return true
				if (user.guildId != null) return true
				val name = (data.guildName.ifBlank { page.draftCreateGuildName }).trim()
				if (name.isBlank()) return true
				if (GuildServices.getGuildByName(name) != null) return true

				if (guildConfig.requireItemToCreate) {
					val requiredItemId = guildConfig.requiredItemIdToCreate.trim()
					val hasItem = InventoryUtil.playerHasItemId(store, ref, requiredItemId)
					if (hasItem != true) return true
				}

				GuildServices.createGuild(name, user.id)
				page.draftCreateGuildName = ""
				cmd.set(page.tabSel("#CreateGuildNameInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"invite_member" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
				if (selfRank != GuildRank.OWNER && selfRank != GuildRank.OFFICER) return true

				val targetName = (data.memberName.ifBlank { page.draftInviteMemberUsername }).trim()
				if (targetName.isBlank()) return true
				val target = UserServices.getUserByUsername(targetName) ?: UserServices.createUser(targetName)
				if (target == null) return true
				if (target.guildId != null) return true

				GuildServices.addMember(guild.id, target.id, GuildRank.MEMBER)
				page.draftInviteMemberUsername = ""
				cmd.set(page.tabSel("#InviteMemberInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"promote_member" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
				if (selfRank != GuildRank.OWNER && selfRank != GuildRank.OFFICER) return true

				val targetName = (data.memberName.ifBlank { page.draftPromoteMemberUsername }).trim()
				if (targetName.isBlank()) return true
				val target = UserServices.getUserByUsername(targetName) ?: return true
				if (target.guildId != guild.id) return true
				if (target.id == user.id) return true

				val targetRank = guild.members.firstOrNull { it.userId == target.id }?.rank ?: GuildRank.MEMBER
				if (targetRank == GuildRank.MEMBER) {
					GuildServices.setRank(guild.id, target.id, GuildRank.OFFICER)
				}
				page.draftPromoteMemberUsername = ""
				cmd.set(page.tabSel("#PromoteMemberInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"demote_member" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
				if (selfRank != GuildRank.OWNER && selfRank != GuildRank.OFFICER) return true

				val targetName = (data.memberName.ifBlank { page.draftDemoteMemberUsername }).trim()
				if (targetName.isBlank()) return true
				val target = UserServices.getUserByUsername(targetName) ?: return true
				if (target.guildId != guild.id) return true
				if (target.id == user.id) return true

				val targetRank = guild.members.firstOrNull { it.userId == target.id }?.rank ?: GuildRank.MEMBER
				if (targetRank == GuildRank.OFFICER) {
					GuildServices.setRank(guild.id, target.id, GuildRank.MEMBER)
				}

				page.draftDemoteMemberUsername = ""
				cmd.set(page.tabSel("#DemoteMemberInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"kick_member" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
				if (selfRank != GuildRank.OWNER && selfRank != GuildRank.OFFICER) return true

				val targetName = (data.memberName.ifBlank { page.draftKickMemberUsername }).trim()
				if (targetName.isBlank()) return true
				val target = UserServices.getUserByUsername(targetName) ?: return true
				if (target.guildId != guild.id) return true
				if (target.id == user.id) return true

				val targetRank = guild.members.firstOrNull { it.userId == target.id }?.rank ?: GuildRank.MEMBER
				if (targetRank == GuildRank.OWNER) return true
				if (selfRank == GuildRank.OFFICER && targetRank == GuildRank.OFFICER) return true

				GuildServices.removeMember(guild.id, target.id)
				page.draftKickMemberUsername = ""
				cmd.set(page.tabSel("#KickMemberInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"leave_guild" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER

				if (selfRank == GuildRank.OWNER) {
					val others = guild.members.filter { it.userId != user.id }
					if (others.isEmpty()) {
						GuildServices.deleteGuild(guild.id)
					} else {
						val newOwnerId = others.first().userId
						GuildServices.setOwner(guild.id, newOwnerId)
						GuildServices.removeMember(guild.id, user.id)
					}
				} else {
					GuildServices.removeMember(guild.id, user.id)
				}

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"delete_guild" -> {
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val selfRank = guild.members.firstOrNull { it.userId == user.id }?.rank ?: GuildRank.MEMBER
				if (selfRank != GuildRank.OWNER) return true

				GuildServices.deleteGuild(guild.id)
				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"send_guild_board" -> {
				val boardConfig = page.configSnapshot.guildBoard
				if (!boardConfig.allowPost) return true
				val user = getOrCreateUser(page) ?: return true
				val guild = user.guildId?.let { GuildServices.getGuildById(it) } ?: return true
				val content = (data.boardText.ifBlank { page.draftGuildBoardText }).trim()
				if (content.isBlank()) return true

				if (boardConfig.requireItemToPost) {
					val requiredItemId = boardConfig.requiredItemIdToPost.trim()
					val hasItem = InventoryUtil.playerHasItemId(store, ref, requiredItemId)
					if (hasItem != true) return true
				}

				GuildServices.addBoardMessage(guild.id, user.id, content)
				page.draftGuildBoardText = ""
				cmd.set(page.tabSel("#GuildBoardInput.Value"), "")

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			else -> return false
		}
	}
}
