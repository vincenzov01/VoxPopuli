package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.veim.voxpopuli.database.*

object DatabaseManager {
    fun init(dbPath: String = "voxpopuli.db") {
        // Connessione a SQLite
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        // Creazione tabelle se non esistono
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Guilds,
                GuildMembers,
                GuildBoardMessages,
                Posts,
                PostLikes,
                Messages,
                Reports,
                Bans,
                AdminActionLogs
            )
        }
    }
}
