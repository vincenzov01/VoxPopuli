package com.voxpopuli.services

import com.voxpopuli.data.PrivateMessage
import java.util.UUID

object MessageServices {
    private val messages = mutableListOf<PrivateMessage>()

    fun getMessagesForPlayer(playerUuid: UUID): List<PrivateMessage> {
        // Convertiamo UUID in String perché la tua data class usa String per gli ID
        val pId = playerUuid.toString()
        return messages.filter { it.recipientId == pId }
    }

    fun sendMessage(senderName: String, senderId: String, recipientId: String, content: String) {
        messages.add(PrivateMessage(
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            content = content
        ))
    }
}