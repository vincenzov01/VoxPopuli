package com.veim.voxpopuli.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.name

object FileAuditLog {
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val lock = Any()

    // Questi valori sono aggiornabili da config
    @Volatile
    private var maxFileBytes: Long = 5L * 1024L * 1024L // default 5 MiB
    @Volatile
    private var maxFilesToKeep: Int = 50

    /** Da chiamare quando la config viene caricata o aggiornata */
    fun updateConfig(logs: com.veim.voxpopuli.config.VoxPopuliConfig.Logs) {
        maxFileBytes = (logs.maxFileMB.coerceAtLeast(1)).toLong() * 1024L * 1024L
        maxFilesToKeep = logs.maxFilesToKeep.coerceAtLeast(1)
    }

    private val logsDir: Path = Path.of("config", "voxpopuli", "logs")
    private val sessionId: String = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now())
    private val filePrefix: String = "audit-$sessionId"
    private var activeIndex: Int = 0

    data class Entry(
        val tsEpochMillis: Long,
        val tsIso: String,
        val scope: String,
        val actorUsername: String,
        val actorUserId: Int,
        val action: String,
        val targetUserId: Int? = null,
        val targetPostId: Int? = null,
        val targetGuildId: Int? = null,
        val details: Map<String, String> = emptyMap(),
    )

    fun logAction(
        scope: String,
        actorUsername: String,
        actorUserId: Int,
        action: String,
        targetUserId: Int? = null,
        targetPostId: Int? = null,
        targetGuildId: Int? = null,
        details: Map<String, String> = emptyMap(),
    ) {
        val now = Instant.now()
        val entry = Entry(
            tsEpochMillis = System.currentTimeMillis(),
            tsIso = now.toString(),
            scope = scope,
            actorUsername = actorUsername,
            actorUserId = actorUserId,
            action = action,
            targetUserId = targetUserId,
            targetPostId = targetPostId,
            targetGuildId = targetGuildId,
            details = details,
        )
        append(entry)
    }

    fun logAdminAction(
        actorUsername: String,
        actorUserId: Int,
        action: String,
        targetUserId: Int? = null,
        targetPostId: Int? = null,
        targetGuildId: Int? = null,
        details: Map<String, String> = emptyMap(),
    ) {
        logAction(
            scope = "admin",
            actorUsername = actorUsername,
            actorUserId = actorUserId,
            action = action,
            targetUserId = targetUserId,
            targetPostId = targetPostId,
            targetGuildId = targetGuildId,
            details = details,
        )
    }

    fun logUserAction(
        actorUsername: String,
        actorUserId: Int,
        action: String,
        targetUserId: Int? = null,
        targetPostId: Int? = null,
        targetGuildId: Int? = null,
        details: Map<String, String> = emptyMap(),
    ) {
        logAction(
            scope = "user",
            actorUsername = actorUsername,
            actorUserId = actorUserId,
            action = action,
            targetUserId = targetUserId,
            targetPostId = targetPostId,
            targetGuildId = targetGuildId,
            details = details,
        )
    }

    private fun currentLogPath(): Path = logsDir.resolve("$filePrefix-${activeIndex.toString().padStart(3, '0')}.jsonl")

    private fun listAuditFilesNewestFirst(): List<Path> {
        if (!Files.exists(logsDir)) return emptyList()
        return try {
            Files.list(logsDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.name.startsWith("audit-") && it.name.endsWith(".jsonl") }
                    .sorted { a, b ->
                        val ta = Files.getLastModifiedTime(a).toMillis()
                        val tb = Files.getLastModifiedTime(b).toMillis()
                        when {
                            ta == tb -> b.name.compareTo(a.name)
                            else -> tb.compareTo(ta)
                        }
                    }
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun cleanupOldFilesLocked() {
        val files = listAuditFilesNewestFirst()
        if (files.size <= maxFilesToKeep) return
        files.drop(maxFilesToKeep).forEach { path ->
            try {
                Files.deleteIfExists(path)
            } catch (_: Exception) {
                // best-effort
            }
        }
    }

    private fun ensureRotationLocked(extraBytesToWrite: Int) {
        try {
            Files.createDirectories(logsDir)
        } catch (_: Exception) {
            return
        }

        while (true) {
            val path = currentLogPath()
            val size = try {
                if (Files.exists(path)) Files.size(path) else 0L
            } catch (_: Exception) {
                0L
            }
            if (size + extraBytesToWrite <= maxFileBytes) return
            activeIndex++
        }
    }

    private fun append(entry: Entry) {
        val line = gson.toJson(entry) + "\n"
        val bytes = line.toByteArray(StandardCharsets.UTF_8)
        synchronized(lock) {
            try {
                ensureRotationLocked(bytes.size)
                val path = currentLogPath()
                Files.write(
                    path,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                )
                cleanupOldFilesLocked()
            } catch (_: Exception) {
                // Non deve mai rompere il gameplay/UI
            }
        }
    }

    fun tailLines(maxLines: Int = 200): List<String> {
        if (maxLines <= 0) return emptyList()

        synchronized(lock) {
            val out = ArrayList<String>(maxLines)
            val files = listAuditFilesNewestFirst()
            if (files.isEmpty()) return emptyList()

            for (file in files) {
                val remaining = maxLines - out.size
                if (remaining <= 0) break
                val chunk = tailLinesFromFileLocked(file, remaining)
                if (chunk.isNotEmpty()) out.addAll(0, chunk) // prepend older chunk
            }

            if (out.size > maxLines) return out.takeLast(maxLines)
            return out
        }
    }

    private fun tailLinesFromFileLocked(path: Path, maxLines: Int): List<String> {
        if (maxLines <= 0) return emptyList()
        if (!Files.exists(path)) return emptyList()

        return try {
            RandomAccessFile(path.toFile(), "r").use { raf ->
                var pointer = raf.length() - 1
                if (pointer < 0) return@use emptyList()

                val lines = ArrayList<String>(maxLines)
                var current = StringBuilder()

                while (pointer >= 0 && lines.size < maxLines) {
                    raf.seek(pointer)
                    val b = raf.readByte().toInt()

                    when (b) {
                        10 -> { // '\n'
                            if (current.isNotEmpty()) {
                                lines.add(current.reverse().toString())
                                current = StringBuilder()
                            }
                        }
                        13 -> {
                            // ignore '\r'
                        }
                        else -> current.append(b.toChar())
                    }
                    pointer--
                }

                if (current.isNotEmpty() && lines.size < maxLines) {
                    lines.add(current.reverse().toString())
                }

                lines.reverse()
                lines
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun tailText(maxLines: Int = 200): String =
        tailLines(maxLines).joinToString("\n")
}