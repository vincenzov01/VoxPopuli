package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID

object GuildServices {

        // --- BACHECA GILDA ---
        fun getBoardMessages(guildId: Int): List<GuildBoardMessage> = transaction {
            GuildBoardMessages.select { GuildBoardMessages.guildId eq guildId }
                .orderBy(GuildBoardMessages.timestamp, SortOrder.DESC)
                .map {
                    GuildBoardMessage(
                        id = it[GuildBoardMessages.id],
                        guildId = it[GuildBoardMessages.guildId],
                        authorId = it[GuildBoardMessages.authorId],
                        content = it[GuildBoardMessages.content],
                        timestamp = it[GuildBoardMessages.timestamp]
                    )
                }
        }

        fun addBoardMessage(guildId: Int, authorId: Int, content: String) = transaction {
            GuildBoardMessages.insert {
                it[GuildBoardMessages.guildId] = guildId
                it[GuildBoardMessages.authorId] = authorId
                it[GuildBoardMessages.content] = content
                it[GuildBoardMessages.timestamp] = System.currentTimeMillis()
            }
        }

        fun getAllBoardMessages(limit: Int = 200): List<GuildBoardMessage> = transaction {
            GuildBoardMessages.selectAll()
                .orderBy(GuildBoardMessages.timestamp, SortOrder.DESC)
                .limit(limit)
                .map {
                    GuildBoardMessage(
                        id = it[GuildBoardMessages.id],
                        guildId = it[GuildBoardMessages.guildId],
                        authorId = it[GuildBoardMessages.authorId],
                        content = it[GuildBoardMessages.content],
                        timestamp = it[GuildBoardMessages.timestamp]
                    )
                }
        }

        fun deleteBoardMessage(messageId: Int, userId: Int, allowAdminBypass: Boolean = false) = transaction {
            if (allowAdminBypass) {
                GuildBoardMessages.deleteWhere { GuildBoardMessages.id eq messageId }
                return@transaction
            }

            // Solo autore o owner può cancellare
            val msg = GuildBoardMessages.select { GuildBoardMessages.id eq messageId }.singleOrNull()
            val guildId = msg?.get(GuildBoardMessages.guildId)
            val authorId = msg?.get(GuildBoardMessages.authorId)
            val guild = guildId?.let { getGuildById(it) }
            if (msg != null && (authorId == userId || (guild != null && guild.ownerId == userId))) {
                GuildBoardMessages.deleteWhere { GuildBoardMessages.id eq messageId }
            }
        }
    fun getGuildById(id: Int): Guild? = transaction {
        Guilds.select { Guilds.id eq id }.mapNotNull {
            val guildId = it[Guilds.id].value
            Guild(
                id = guildId,
                name = it[Guilds.name],
                ownerId = it[Guilds.ownerId],
                members = GuildMembers.select { GuildMembers.guildId eq guildId }.map { gm ->
                    GuildMember(
                        userId = gm[GuildMembers.userId],
                        rank = GuildRank.valueOf(gm[GuildMembers.rank])
                    )
                }
            )
        }.singleOrNull()
    }

    fun getGuildByName(name: String): Guild? = transaction {
        Guilds.select { Guilds.name eq name }.mapNotNull {
            getGuildById(it[Guilds.id].value)
        }.singleOrNull()
    }

    fun getGuildsForUser(userId: Int): List<Guild> = transaction {
        GuildMembers.select { GuildMembers.userId eq userId }.mapNotNull {
            getGuildById(it[GuildMembers.guildId])
        }
    }

    fun getAllGuilds(limit: Int = 200): List<Guild> = transaction {
        Guilds.selectAll()
            .orderBy(Guilds.id, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                val guildId = row[Guilds.id].value
                Guild(
                    id = guildId,
                    name = row[Guilds.name],
                    ownerId = row[Guilds.ownerId],
                    members = GuildMembers.select { GuildMembers.guildId eq guildId }.map { gm ->
                        GuildMember(
                            userId = gm[GuildMembers.userId],
                            rank = GuildRank.valueOf(gm[GuildMembers.rank])
                        )
                    }
                )
            }
    }

    fun createGuild(name: String, ownerId: Int): Guild? = transaction {
        val guildId = Guilds.insertAndGetId { row ->
            row[Guilds.name] = name
            row[Guilds.ownerId] = ownerId
        }?.value
        if (guildId != null) {
            GuildMembers.insert { row ->
                row[GuildMembers.userId] = ownerId
                row[GuildMembers.guildId] = guildId
                row[GuildMembers.rank] = GuildRank.OWNER.name
            }

            Users.update({ Users.id eq EntityID(ownerId, Users) }) { row ->
                row[Users.guildId] = guildId
            }
        }
        guildId?.let { getGuildById(it) }
    }

    fun addMember(guildId: Int, userId: Int, rank: GuildRank = GuildRank.MEMBER) = transaction {
        GuildMembers.insertIgnore {
            it[GuildMembers.userId] = userId
            it[GuildMembers.guildId] = guildId
            it[GuildMembers.rank] = rank.name
        }

        Users.update({ Users.id eq EntityID(userId, Users) }) { row ->
            row[Users.guildId] = guildId
        }
    }

    fun removeMember(guildId: Int, userId: Int) = transaction {
        GuildMembers.deleteWhere { (GuildMembers.guildId eq guildId) and (GuildMembers.userId eq userId) }

        Users.update({ Users.id eq EntityID(userId, Users) and (Users.guildId eq guildId) }) { row ->
            row[Users.guildId] = null
        }
    }

    fun setRank(guildId: Int, userId: Int, rank: GuildRank) = transaction {
        GuildMembers.update({ (GuildMembers.guildId eq guildId) and (GuildMembers.userId eq userId) }) {
            it[GuildMembers.rank] = rank.name
        }
    }

    fun setOwner(guildId: Int, newOwnerId: Int) = transaction {
        Guilds.update({ Guilds.id eq guildId }) {
            it[ownerId] = newOwnerId
        }
        setRank(guildId, newOwnerId, GuildRank.OWNER)
    }

    fun transferOwnership(guildId: Int, newOwnerId: Int) = transaction {
        val currentOwnerId = Guilds
            .select { Guilds.id eq guildId }
            .singleOrNull()
            ?.get(Guilds.ownerId)

        if (currentOwnerId != null && currentOwnerId != newOwnerId) {
            setRank(guildId, currentOwnerId, GuildRank.OFFICER)
        }

        Guilds.update({ Guilds.id eq guildId }) {
            it[ownerId] = newOwnerId
        }
        setRank(guildId, newOwnerId, GuildRank.OWNER)
    }

    fun deleteGuild(guildId: Int) = transaction {
        // Unset guild for all members
        val memberIds = GuildMembers.select { GuildMembers.guildId eq guildId }
            .map { it[GuildMembers.userId] }

        for (userId in memberIds) {
            Users.update({ Users.id eq EntityID(userId, Users) and (Users.guildId eq guildId) }) { row ->
                row[Users.guildId] = null
            }
        }

        // Remove board messages
        GuildBoardMessages.deleteWhere { GuildBoardMessages.guildId eq guildId }

        GuildMembers.deleteWhere { GuildMembers.guildId eq guildId }
        Guilds.deleteWhere { Guilds.id eq guildId }
    }
}
