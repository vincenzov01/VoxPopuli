package com.veim.voxpopuli.ui.tabs.admin

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.database.MessageServices
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.FileAuditLog
import java.text.SimpleDateFormat
import java.util.Date

class MessagesTab(
    private val page: VoxPopuliAdminDashboardPage,
) : AdminTab {
    override val tabId: String = VoxPopuliAdminDashboardPage.TAB_MESSAGES
    private var messagesStatus: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    override fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        cmd.clear("#AdminMessageList")
        val messages = MessageServices.getAllMessages(limit = 200)
        messagesStatus = "${messages.size} messaggi"

        messages.forEachIndexed { index, message ->
            cmd.append("#AdminMessageList", "voxpopuli/admin/AdminMessageItem.ui")
            val selector = "#AdminMessageList[$index]"
            val sender = UserServices.getUserById(message.senderId)?.username ?: "?"
            val receiver = UserServices.getUserById(message.receiverId)?.username ?: "?"
            cmd.set("$selector #MessageSender.Text", "$sender -> $receiver")
            cmd.set("$selector #MessageTimestamp.Text", dateFormat.format(Date(message.timestamp)))
            cmd.set("$selector #MessageContent.Text", message.content)

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                page.uiEventData(action = "delete_message_admin", id = message.id),
                false,
            )
        }
        cmd.set("#MessagesStatusLabel.Text", messagesStatus)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshMessagesButton", page.uiEventData("refresh_messages"), false)
    }

    override fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String? =
        when (data.action) {
            "refresh_messages" -> {
                "Aggiornato"
            }

            "delete_message_admin" -> {
                val id = data.id
                if (id > 0) {
                    MessageServices.deleteMessage(id, userId = user.id, allowAdminBypass = true)
                    FileAuditLog.logAdminAction(
                        actorUsername = page.player.username,
                        actorUserId = user.id,
                        action = "message.delete",
                        details = mapOf("messageId" to id.toString()),
                    )
                    "Messaggio eliminato"
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }
}
