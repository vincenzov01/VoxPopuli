package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.veim.voxpopuli.database.*
import java.nio.file.Files
import java.nio.file.Path

object DatabaseManager {
    fun init(dbPath: String = "voxpopuli.db") {
        try {
            val parent = Path.of(dbPath).parent
            if (parent != null) Files.createDirectories(parent)
        } catch (_: Exception) {
            // Ignore directory creation errors; connection attempt will surface issues if any.
        }
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
                AdminActionLogs,
                VoxPopuliPluginConfig
            )
        }
    }
}
