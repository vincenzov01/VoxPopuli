package com.voxpopuli.managers

import com.voxpopuli.data.*
import org.jetbrains.exposed.sql.*
// QUESTO IMPORT RISOLVE L'ERRORE 'PLUS'
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID

// TABELLA POST (Speculare a Post in DataModels.kt)
object PostsTable : Table("posts") {
    val id = varchar("id", 36)
    val authorName = varchar("author_name", 64)
    val authorUuid = varchar("author_uuid", 36)
    val content = text("content")
    val category = varchar("category", 32)
    val likes = integer("likes").default(0)
    val formattedTime = varchar("formatted_time", 32)
    override val primaryKey = PrimaryKey(id)
}

// TABELLA GILDA (Speculare a Guild in DataModels.kt)
object GuildsTable : Table("guilds") {
    val id = varchar("id", 36)
    val name = varchar("name", 64)
    val tag = varchar("tag", 8)
    val motto = varchar("motto", 128)
    val level = integer("level").default(1)
    override val primaryKey = PrimaryKey(id)
}

// TABELLA MEMBRI (Per gestire la List<GuildMember> di DataModels)
object GuildMembersTable : Table("guild_members") {
    val guildId = varchar("guild_id", 36)
    val userUuid = varchar("user_uuid", 36)
    val username = varchar("username", 64)
    val rankName = varchar("rank_name", 32)
}

object DatabaseManager {

    fun connect() {
        // Connessione a SQLite locale (crea il file voxpopuli.db se non esiste)
        Database.connect("jdbc:sqlite:voxpopuli.db", "org.sqlite.JDBC")

        transaction {
            // Crea le tabelle se non esistono nel database
            SchemaUtils.create(PostsTable, GuildsTable, GuildMembersTable)
        }
    }

    // --- LOGICA POST ---

    fun savePost(post: Post) = transaction {
        PostsTable.insert {
            it[id] = post.id
            it[authorName] = post.authorName
            it[authorUuid] = post.authorUuid.toString()
            it[content] = post.content
            it[category] = post.category.name
            it[likes] = post.likes
            it[formattedTime] = post.formattedTime
        }
    }

    fun getAllPosts(): List<Post> = transaction {
        PostsTable.selectAll()
            .orderBy(PostsTable.id, SortOrder.DESC) // O per data se aggiungi una colonna timestamp
            .map {
                Post(
                    id = it[PostsTable.id],
                    authorName = it[PostsTable.authorName],
                    authorUuid = UUID.fromString(it[PostsTable.authorUuid]),
                    content = it[PostsTable.content],
                    category = PostCategory.valueOf(it[PostsTable.category]),
                    formattedTime = it[PostsTable.formattedTime],
                    likes = it[PostsTable.likes],
                    comments = emptyList()
                )
            }
    }

    fun updatePostLikes(postId: String) = transaction {
        PostsTable.update({ PostsTable.id eq postId }) {
            it.update(PostsTable.likes, PostsTable.likes plus 1)
        }
    }

    // --- LOGICA GILDA ---

    fun saveGuild(guild: Guild) = transaction {
        // 1. Salva i dati base della gilda
        GuildsTable.insert {
            it[id] = guild.id.toString()
            it[name] = guild.name
            it[tag] = guild.tag
            it[motto] = guild.motto
            it[level] = guild.level
        }

        // 2. Salva ogni membro della lista (Risolve l'errore della List)
        guild.members.forEach { member ->
            GuildMembersTable.insert {
                it[guildId] = guild.id.toString()
                it[userUuid] = member.uuid.toString()
                it[username] = member.username
                it[rankName] = member.rankName
            }
        }
    }
}