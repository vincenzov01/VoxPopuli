package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.database.MessageServices
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage
import com.veim.voxpopuli.util.FileAuditLog
import com.veim.voxpopuli.util.InventoryUtil
import java.text.SimpleDateFormat
import java.util.Date

object MissiveTab : BaseVoxTab(id = "missive", title = "Missive") {
	override val templatePath: String = "voxpopuli/missive.ui"

	override fun bindEvents(page: VoxPopuliDashboardPage, evt: UIEventBuilder) {
		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#MessageRecipientInput"),
			page.uiEventData(action = "draft_message_recipient"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#MessageRecipientInput"),
			page.uiEventData(action = "draft_message_recipient"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#MessageRecipientInput"),
			page.uiEventData(action = "draft_message_recipient"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.ValueChanged,
			page.tabSel("#MessageTextInput"),
			page.uiEventData(action = "draft_message_text"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
			page.tabSel("#MessageTextInput"),
			page.uiEventData(action = "draft_message_text"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
			page.tabSel("#MessageTextInput"),
			page.uiEventData(action = "draft_message_text"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Activating,
			page.tabSel("#SendMessageButton"),
			page.uiEventData(
				action = "send_message",
				captures = mapOf(
					"Recipient" to page.tabSel("#MessageRecipientInput.Value"),
					"Message" to page.tabSel("#MessageTextInput.Value"),
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
		cmd.clear(page.tabSel("#MessageList"))
		val allowDelete = page.configSnapshot.messages.allowDelete

		val user = getOrCreateUser(page) ?: return
		val messages = MessageServices.getMessagesForUser(user.id)
		cmd.set("#ContentTitle.Text", "${title} (${messages.size})")
		val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

		messages.forEachIndexed { index, message ->
			cmd.append(page.tabSel("#MessageList"), "voxpopuli/MessageItem.ui")
			val selector = page.tabSel("#MessageList[${index}]")

			val senderName = UserServices.getUserById(message.senderId)?.username ?: "?"
			val receiverName = UserServices.getUserById(message.receiverId)?.username ?: "?"
			val header = if (message.senderId == user.id) "A: $receiverName" else senderName

			cmd.set("$selector #MessageSender.Text", header)
			cmd.set("$selector #MessageTimestamp.Text", dateFormat.format(Date(message.timestamp)))
			cmd.set("$selector #MessageContent.Text", message.content)
			cmd.set("$selector #DeleteButton.Visible", allowDelete)

			evt.addEventBinding(
				CustomUIEventBindingType.Activating,
				"$selector #ReplyButton",
				page.uiEventData(action = "reply", postId = message.id),
				false
			)
			if (allowDelete) {
				evt.addEventBinding(
					CustomUIEventBindingType.Activating,
					"$selector #DeleteButton",
					page.uiEventData(action = "delete_message", postId = message.id),
					false
				)
			}
		}

		// Keep inputs in sync (useful when switching tabs)
		cmd.set(page.tabSel("#MessageRecipientInput.Value"), page.draftMessageRecipient)
		cmd.set(page.tabSel("#MessageTextInput.Value"), page.draftMessageText)
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
			"draft_message_recipient" -> {
				page.draftMessageRecipient = data.text
				return true
			}
			"draft_message_text" -> {
				page.draftMessageText = data.text
				return true
			}
			"send_message" -> {
				val config = page.configSnapshot.messages
				val sender = getOrCreateUser(page) ?: return true
				val recipientName = (data.recipient.ifBlank { page.draftMessageRecipient }).trim()
				val content = (data.message.ifBlank { page.draftMessageText }).trim()
				if (recipientName.isBlank() || content.isBlank()) return true

				if (config.requireItemToSend) {
					val requiredItemId = config.requiredItemIdToSend.trim()
					val hasItem = InventoryUtil.playerHasItemId(store, ref, requiredItemId)
					if (hasItem != true) return true
				}

				val receiver = UserServices.getUserByUsername(recipientName) ?: UserServices.createUser(recipientName)
				if (receiver != null) {
					FileAuditLog.logUserAction(
						actorUsername = page.player.username,
						actorUserId = sender.id,
						action = "message.send",
						targetUserId = receiver.id,
						details = mapOf(
							"receiverUsername" to receiver.username,
							"contentLen" to content.length.toString(),
						),
					)
					MessageServices.sendMessage(sender.id, receiver.id, content)
					page.draftMessageText = ""
					cmd.set(page.tabSel("#MessageTextInput.Value"), "")
				}

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"reply" -> {
				val user = getOrCreateUser(page) ?: return true
				val messageId = data.postId
				if (messageId < 0) return true

				val message = MessageServices.getMessageById(messageId) ?: return true
				val otherUserId = if (message.senderId == user.id) message.receiverId else message.senderId
				val otherName = UserServices.getUserById(otherUserId)?.username ?: ""

				page.draftMessageRecipient = otherName
				cmd.set(page.tabSel("#MessageRecipientInput.Value"), otherName)

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			"delete_message" -> {
				if (!page.configSnapshot.messages.allowDelete) return true
				val user = getOrCreateUser(page) ?: return true
				val messageId = data.postId
				if (messageId < 0) return true
				val msg = MessageServices.getMessageById(messageId)
				val otherUserId = if (msg != null) {
					if (msg.senderId == user.id) msg.receiverId else msg.senderId
				} else {
					null
				}
				FileAuditLog.logUserAction(
					actorUsername = page.player.username,
					actorUserId = user.id,
					action = "message.delete",
					targetUserId = otherUserId,
					details = mapOf("messageId" to messageId.toString()),
				)
				MessageServices.deleteMessage(messageId, user.id)

				apply(cmd)
				render(page, ref, store, cmd, evt)
				return true
			}
			else -> return false
		}
	}
}
