package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object MessageServices {
    fun getMessageById(messageId: Int): Message? = transaction {
        Messages.select { Messages.id eq messageId }
            .limit(1)
            .map {
                Message(
                    id = it[Messages.id],
                    senderId = it[Messages.senderId],
                    receiverId = it[Messages.receiverId],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp]
                )
            }
            .singleOrNull()
    }

    fun getMessagesForUser(userId: Int): List<Message> = transaction {
        Messages.select { (Messages.senderId eq userId) or (Messages.receiverId eq userId) }
            .orderBy(Messages.timestamp, SortOrder.DESC)
            .map {
                Message(
                    id = it[Messages.id],
                    senderId = it[Messages.senderId],
                    receiverId = it[Messages.receiverId],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp]
                )
            }
    }

    fun getAllMessages(limit: Int = 200): List<Message> = transaction {
        Messages.selectAll()
            .orderBy(Messages.timestamp, SortOrder.DESC)
            .limit(limit)
            .map {
                Message(
                    id = it[Messages.id],
                    senderId = it[Messages.senderId],
                    receiverId = it[Messages.receiverId],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp]
                )
            }
    }

    fun sendMessage(senderId: Int, receiverId: Int, content: String) {
        transaction {
            Messages.insert {
                it[Messages.senderId] = senderId
                it[Messages.receiverId] = receiverId
                it[Messages.content] = content
                it[Messages.timestamp] = System.currentTimeMillis()
            }
        }
    }

    fun deleteMessage(messageId: Int, userId: Int, allowAdminBypass: Boolean = false) {
        transaction {
            if (allowAdminBypass) {
                Messages.deleteWhere { Messages.id eq messageId }
                return@transaction
            }

            // Only sender or receiver can delete
            Messages.deleteWhere {
                (Messages.id eq messageId) and ((Messages.senderId eq userId) or (Messages.receiverId eq userId))
            }
        }
    }
}
