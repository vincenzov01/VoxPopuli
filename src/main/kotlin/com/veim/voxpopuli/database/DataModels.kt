package com.veim.voxpopuli.database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.dao.id.IntIdTable
// --- BACHECA GILDA ---
data class GuildBoardMessage(
    val id: Int,
    val guildId: Int,
    val authorId: Int,
    val content: String,
    val timestamp: Long
)

object GuildBoardMessages : Table() {
    val id = integer("id").autoIncrement()
    val guildId = integer("guild_id")
    val authorId = integer("author_id")
    val content = varchar("content", 1024)
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(id)
}

// --- UTENTE E PROFILO ---
data class User(
    val id: Int,
    val username: String,
    val guildId: Int? = null,
    val isAdmin: Boolean = false
)

// --- GILDE ---
data class Guild(
    val id: Int,
    val name: String,
    val ownerId: Int,
    val members: List<GuildMember> = emptyList()
)

enum class GuildRank { OWNER, OFFICER, MEMBER }

data class GuildMember(
    val userId: Int,
    val rank: GuildRank
)

data class Post(
    val id: Int,
    val authorId: Int,
    val content: String,
    val timestamp: Long,
    val likedBy: Set<Int> = emptySet() // userId unici che hanno messo like
)

data class Message(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long
)

// --- BACHECA GILDA ---
data class Report(
    val reporterId: Int,
    val reportedUserId: Int?, // può essere null se si segnala un post
    val postId: Int?, // può essere null se si segnala un utente
    val reason: String,
    val timestamp: Long,
    val resolved: Boolean = false,
    val resolvedBy: Int? = null // admin che ha gestito la segnalazione
)

data class Ban(
    val id: Int,
    val userId: Int,
    val reason: String,
    val bannedBy: Int, // admin
    val timestamp: Long,
    val duration: Long? = null // null = permanente
)

data class AdminActionLog(
    val id: Int,
    val adminId: Int,
    val action: String,
    val targetUserId: Int?,
    val targetPostId: Int?,
    val timestamp: Long
)


// --- EXPOSED TABLES ---
object Users : IntIdTable("users") {
    val username = varchar("username", 32)
    val guildId = integer("guild_id").nullable()
    val isAdmin = bool("is_admin").default(false)
}

object Guilds : IntIdTable("guilds") {
    val name = varchar("name", 64)
    val ownerId = integer("owner_id")
}

object Posts : IntIdTable("posts") {
    val authorId = integer("author_id")
    val content = varchar("content", 1024)
    val timestamp = long("timestamp")
}

object GuildMembers : Table() {
    val userId = integer("user_id")
    val guildId = integer("guild_id")
    val rank = varchar("rank", 16)
    override val primaryKey = PrimaryKey(userId, guildId)
}

object Messages : Table() {
    val id = integer("id").autoIncrement()
    val senderId = integer("sender_id")
    val receiverId = integer("receiver_id")
    val content = varchar("content", 1024)
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(id)
}

object Reports : Table() {
    val id = integer("id").autoIncrement()
    val reporterId = integer("reporter_id")
    val reportedUserId = integer("reported_user_id").nullable()
    val postId = integer("post_id").nullable()
    val reason = varchar("reason", 255)
    val timestamp = long("timestamp")
    val resolved = bool("resolved").default(false)
    val resolvedBy = integer("resolved_by").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Bans : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val reason = varchar("reason", 255)
    val bannedBy = integer("banned_by")
    val timestamp = long("timestamp")
    val duration = long("duration").nullable()
    override val primaryKey = PrimaryKey(id)
}

object AdminActionLogs : Table() {
    val id = integer("id").autoIncrement()
    val adminId = integer("admin_id")
    val action = varchar("action", 255)
    val targetUserId = integer("target_user_id").nullable()
    val targetPostId = integer("target_post_id").nullable()
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(id)
}

// --- POST LIKES ---
object PostLikes : Table() {
    val postId = integer("post_id")
    val userId = integer("user_id")
    override val primaryKey = PrimaryKey(postId, userId)
}
