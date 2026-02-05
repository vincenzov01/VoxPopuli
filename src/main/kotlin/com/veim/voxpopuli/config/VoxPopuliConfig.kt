package com.veim.voxpopuli.config

/**
 * Lightweight JSON-backed configuration.
 *
 * This is intentionally independent from Hytale's Config<T> helper so it can work
 * even when API details differ between server builds.
 */
data class VoxPopuliConfig(
    var database: Database = Database(),
) {
    data class Database(
        /** SQLite db file path. Relative paths are resolved from the server working directory. */
        var path: String = "config/voxpopuli/voxpopuli.db",
    )
}
