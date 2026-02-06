package com.veim.voxpopuli.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.universe.PlayerRef
import java.io.File
import java.util.UUID
import java.util.logging.Level

object OpPermissionsUtil {
    private val LOGGER = HytaleLogger.forEnclosingClass()
    private val gson = Gson()

    private data class PermissionsRoot(
        val users: Map<String, PermissionsUser>? = null,
        val groups: Map<String, List<String>>? = null,
    )

    private data class PermissionsUser(
        val groups: List<String>? = null,
    )

    /**
     * Returns true if the player is in OP (or equivalent wildcard group), false if not.
     * Returns null if OP status can't be determined (permissions.json not found / UUID not accessible).
     */
    fun isOp(playerRef: PlayerRef): Boolean? {
        val uuid = extractPlayerUuid(playerRef) ?: return null
        return isOp(uuid)
    }

    fun isOp(playerUuid: UUID): Boolean? = isOp(playerUuid.toString())

    fun isOp(playerUuid: String): Boolean? {
        val permissionsFile = findPermissionsFile() ?: return null

        val root = try {
            gson.fromJson(permissionsFile.readText(Charsets.UTF_8), PermissionsRoot::class.java)
        } catch (e: JsonSyntaxException) {
            LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Invalid permissions.json format at %s", permissionsFile.absolutePath)
            return null
        } catch (e: Exception) {
            LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Failed to read permissions.json at %s", permissionsFile.absolutePath)
            return null
        }

        val userEntry = root.users?.get(playerUuid) ?: return false
        val userGroups = userEntry.groups.orEmpty()

        if (userGroups.any { it.equals("OP", ignoreCase = true) }) return true

        val groupPerms = root.groups.orEmpty()
        return userGroups.any { groupName ->
            val perms = groupPerms[groupName]
            perms?.any { it == "*" } == true
        }
    }

    private fun findPermissionsFile(): File? {
        val workingDir = File(System.getProperty("user.dir") ?: ".").absoluteFile

        val candidates = ArrayList<File>(32)
        var current: File? = workingDir
        repeat(8) {
            val base = current ?: return@repeat
            candidates.add(File(base, "permissions.json"))
            candidates.add(File(base, "Server/permissions.json"))
            candidates.add(File(base, "Server/Server/permissions.json"))
            candidates.add(File(base, "config/permissions.json"))
            current = base.parentFile
        }

        return candidates.firstOrNull { it.isFile }
    }

    private fun extractPlayerUuid(playerRef: PlayerRef): String? {
        val direct = readNoArgMember(playerRef, listOf("getUuid", "getUUID", "getUniqueId", "getUniqueID", "uuid", "UUID"))
        if (direct != null) {
            when (direct) {
                is UUID -> return direct.toString()
                else -> {
                    val asString = direct.toString()
                    if (UUID_REGEX.matches(asString)) return asString
                }
            }
        }

        // Try fields as a fallback.
        val fieldValue = readField(playerRef, listOf("uuid", "UUID", "uniqueId", "uniqueID"))
        if (fieldValue != null) {
            when (fieldValue) {
                is UUID -> return fieldValue.toString()
                else -> {
                    val asString = fieldValue.toString()
                    if (UUID_REGEX.matches(asString)) return asString
                }
            }
        }

        return null
    }

    private fun readNoArgMember(target: Any, names: List<String>): Any? {
        val clazz = target.javaClass
        for (name in names) {
            try {
                val method = clazz.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
                    ?: clazz.methods.firstOrNull { it.name.equals(name, ignoreCase = true) && it.parameterCount == 0 }
                if (method != null) {
                    return method.invoke(target)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
        return null
    }

    private fun readField(target: Any, names: List<String>): Any? {
        val clazz = target.javaClass
        for (name in names) {
            try {
                val field = clazz.declaredFields.firstOrNull { it.name == name }
                    ?: clazz.declaredFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (field != null) {
                    field.isAccessible = true
                    return field.get(target)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
        return null
    }

    private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
}
