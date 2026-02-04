package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.dao.id.EntityID

object UserServices {
    fun createUser(username: String, guildId: Int? = null, isAdmin: Boolean = false): User? {
        var userId: Int? = null
        transaction {
            userId = Users.insertAndGetId { row ->
                row[Users.username] = username
                row[Users.guildId] = guildId
                row[Users.isAdmin] = isAdmin
            }?.value
        }
        return userId?.let { getUserById(it) }
    }

    fun getUserById(id: Int): User? = transaction {
        Users.select { Users.id eq id }.mapNotNull {
            User(
                id = it[Users.id].value,
                username = it[Users.username],
                guildId = it[Users.guildId],
                isAdmin = it[Users.isAdmin]
            )
        }.singleOrNull()
    }

    fun getUserByUsername(username: String): User? = transaction {
        Users.select { Users.username eq username }.mapNotNull {
            User(
                id = it[Users.id].value,
                username = it[Users.username],
                guildId = it[Users.guildId],
                isAdmin = it[Users.isAdmin]
            )
        }.singleOrNull()
    }

    fun updateUser(user: User): Boolean = transaction {
        Users.update({ Users.id eq user.id }) { row ->
            row[Users.username] = user.username
            row[Users.guildId] = user.guildId
            row[Users.isAdmin] = user.isAdmin
        } > 0
    }

    fun deleteUser(id: Int): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }

    fun getAllUsers(): List<User> = transaction {
        Users.selectAll().map {
            User(
                id = it[Users.id].value,
                username = it[Users.username],
                guildId = it[Users.guildId],
                isAdmin = it[Users.isAdmin]
            )
        }
    }
}
