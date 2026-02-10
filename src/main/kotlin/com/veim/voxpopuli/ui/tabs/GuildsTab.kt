package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.database.*
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.FileAuditLog
import java.text.SimpleDateFormat
import java.util.Date

class GuildsTab(
    private val page: VoxPopuliAdminDashboardPage,
) : AdminTab {
    override val tabId: String = VoxPopuliAdminDashboardPage.TAB_GUILDS

    private var guildsStatus: String = ""
    private var guildBoardStatus: String = ""
    private var selectedGuildId: Int = -1
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    override fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        renderGuilds(cmd, evt)

        // Render della sezione destra (Membri)
        if (selectedGuildId > 0) {
            renderGuildMembers(cmd, evt, selectedGuildId)
        } else {
            cmd.clear("#AdminGuildMembersList")
            cmd.set("#SelectedGuildLabel.Text", "Membri gilda: (nessuna)")
        }

        // Render della sezione Bacheca
        renderGuildBoard(cmd, evt)

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshGuildsButton", page.uiEventData("refresh_guilds"), false)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshGuildBoardButton", page.uiEventData("refresh_guild_board"), false)
    }

    private fun renderGuilds(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        cmd.clear("#AdminGuildList")
        val guilds = GuildServices.getAllGuilds(limit = 200)
        guildsStatus = "${guilds.size} gilde"
        cmd.set("#GuildsStatusLabel.Text", guildsStatus)

        guilds.forEachIndexed { index, guild ->
            cmd.append("#AdminGuildList", "voxpopuli/AdminGuildItem.ui")
            val selector = "#AdminGuildList[$index]"
            val ownerName = UserServices.getUserById(guild.ownerId)?.username ?: "?"
            cmd.set("$selector #GuildName.Text", guild.name)
            cmd.set("$selector #GuildMeta.Text", "Owner: $ownerName | Membri: ${guild.members.size}")

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #OpenButton",
                page.uiEventData(action = "open_guild", tab = tabId, guildId = guild.id),
                false,
            )
            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                page.uiEventData(action = "delete_guild_admin", tab = tabId, guildId = guild.id),
                false,
            )
        }
    }

    private fun sortedMembersForAdmin(members: List<GuildMember>): List<GuildMember> =
        members.sortedWith(
            compareBy<GuildMember>({
                when (it.rank) {
                    GuildRank.OWNER -> 0
                    GuildRank.OFFICER -> 1
                    GuildRank.MEMBER -> 2
                }
            }).thenBy { it.userId },
        )

    private fun renderGuildMembers(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
        guildId: Int,
    ) {
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
            val selector = "#AdminGuildMembersList[$index]"
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
                    page.uiEventData(action = "set_owner_admin", tab = tabId, guildId = guild.id, userId = member.userId),
                    false,
                )
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #KickButton",
                    page.uiEventData(action = "kick_member_admin", tab = tabId, guildId = guild.id, userId = member.userId),
                    false,
                )
            }

            if (member.rank == GuildRank.MEMBER) {
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #PromoteButton",
                    page.uiEventData(action = "promote_member_admin", tab = tabId, guildId = guild.id, userId = member.userId),
                    false,
                )
            }
            if (member.rank == GuildRank.OFFICER) {
                evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "$selector #DemoteButton",
                    page.uiEventData(action = "demote_member_admin", tab = tabId, guildId = guild.id, userId = member.userId),
                    false,
                )
            }
        }
    }

    private fun renderGuildBoard(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
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
            val selector = "#AdminGuildBoardList[$index]"
            val author = UserServices.getUserById(message.authorId)?.username ?: "?"
            cmd.set("$selector #GuildName.Text", guildName)
            cmd.set("$selector #BoardAuthor.Text", author)
            cmd.set("$selector #BoardTimestamp.Text", dateFormat.format(Date(message.timestamp)))
            cmd.set("$selector #BoardContent.Text", message.content)

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                page.uiEventData(action = "delete_guild_board_admin", id = message.id),
                false,
            )
        }
        cmd.set("#GuildBoardStatusLabel.Text", guildBoardStatus)
    }

    override fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String? =
        when (data.action) {
            "refresh_guilds" -> {
                "Aggiornato"
            }

            "refresh_guild_board" -> {
                "Aggiornato"
            }

            "open_guild" -> {
                val gid = data.guildId
                if (gid > 0) {
                    selectedGuildId = gid
                    "Gilda aperta"
                } else {
                    null
                }
            }

            "delete_guild_admin" -> {
                val gid = data.guildId
                if (gid > 0) {
                    GuildServices.deleteGuild(gid)
                    if (selectedGuildId == gid) selectedGuildId = -1
                    FileAuditLog.logAdminAction(page.player.username, user.id, "guild.delete", targetGuildId = gid)
                    "Gilda eliminata"
                } else {
                    null
                }
            }

            "kick_member_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    GuildServices.removeMember(gid, uid)
                    FileAuditLog.logAdminAction(page.player.username, user.id, "guild.member.kick", targetGuildId = gid, targetUserId = uid)
                    "Membro rimosso"
                } else {
                    null
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
                        FileAuditLog.logAdminAction(
                            page.player.username,
                            user.id,
                            "guild.member.promote",
                            targetGuildId = gid,
                            targetUserId = uid,
                            details =
                                mapOf("rank" to "OFFICER"),
                        )
                        "Promosso"
                    } else {
                        null
                    }
                } else {
                    null
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
                        FileAuditLog.logAdminAction(
                            page.player.username,
                            user.id,
                            "guild.member.demote",
                            targetGuildId = gid,
                            targetUserId = uid,
                            details =
                                mapOf("rank" to "MEMBER"),
                        )
                        "Retrocesso"
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            "set_owner_admin" -> {
                val gid = data.guildId
                val uid = data.userId
                if (gid > 0 && uid > 0) {
                    GuildServices.transferOwnership(gid, uid)
                    selectedGuildId = gid
                    FileAuditLog.logAdminAction(page.player.username, user.id, "guild.owner.set", targetGuildId = gid, targetUserId = uid)
                    "Owner cambiato"
                } else {
                    null
                }
            }

            "delete_guild_board_admin" -> {
                val id = data.id
                if (id > 0) {
                    GuildServices.deleteBoardMessage(id, userId = user.id, allowAdminBypass = true)
                    FileAuditLog.logAdminAction(
                        page.player.username,
                        user.id,
                        "guild.board.delete",
                        details =
                            mapOf("messageId" to id.toString()),
                    )
                    "Comunicazione eliminata"
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }
}
