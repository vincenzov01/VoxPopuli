package com.voxpopuli.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm - dd MMM")
    .withZone(ZoneId.systemDefault())

// ========================================
// POST & CHRONICLE
// ========================================

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long = Instant.now().toEpochMilli()
) {
    val formattedTime: String
        get() = timeFormatter.format(Instant.ofEpochMilli(timestamp))
}

// ========================================
// MESSAGES
// ========================================

data class PrivateMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long = Instant.now().toEpochMilli(),
    val read: Boolean = false
) {
    // AGGIUNGI QUESTA PROPRIETÀ PER RISOLVERE L'ERRORE
    val formattedTime: String
        get() = timeFormatter.format(Instant.ofEpochMilli(timestamp))
}

data class ConversationKey(
    val player1: String,
    val player2: String
) {
    companion object {
        fun create(p1: String, p2: String): ConversationKey {
            return if (p1 < p2) ConversationKey(p1, p2) else ConversationKey(p2, p1)
        }
    }
}

// ========================================
// GUILD
// ========================================

enum class GuildRank(val displayName: String, val priority: Int) {
    MASTER("Maestro", 4),
    OFFICER("Ufficiale", 3),
    VETERAN("Veterano", 2),
    MEMBER("Membro", 1),
    RECRUIT("Recluta", 0)
}

// Risolve gli errori: formattedTime, likes, content, category
data class Post(
    val id: String,
    val authorName: String,
    val authorUuid: UUID,
    val content: String,
    val category: PostCategory,
    val formattedTime: String,
    val likes: Int = 0,
    val comments: List<String> = emptyList()
)

enum class PostCategory { GENERAL, QUEST, BATTLE, EXPLORATION, TRADE, ACHIEVEMENT, GUILD, PERSONAL }

// Risolve gli errori: name, tag, motto, level, members
data class Guild(
    val id: UUID,
    val name: String,
    val tag: String,
    val motto: String,
    val level: Int,
    val members: List<GuildMember>
) {
    // Risolve l'errore: getRankOf
    fun getRankOf(playerUuid: UUID): String {
        return members.find { it.uuid == playerUuid }?.rankName ?: "Ospite"
    }
}

// Risolve gli errori: username, rankName
data class GuildMember(
    val uuid: UUID,
    val username: String,
    val rankName: String
)