package com.veim.voxpopuli.config

/**
 * Lightweight JSON-backed configuration.
 *
 * This is intentionally independent from Hytale's Config<T> helper so it can work
 * even when API details differ between server builds.
 */
data class VoxPopuliConfig(
    var database: Database = Database(),
    var tabs: Tabs = Tabs(),
    var posts: Posts = Posts(),
    var messages: Messages = Messages(),
    var guilds: Guilds = Guilds(),
    var guildBoard: GuildBoard = GuildBoard(),
) {
    data class Database(
        /** SQLite db file path. Relative paths are resolved from the server working directory. */
        var path: String = "config/voxpopuli/voxpopuli.db",
    )

    data class Tabs(
        var enableCronache: Boolean = true,
        var enableMissive: Boolean = true,
        var enableGilda: Boolean = true,
    )

    data class Posts(
        /** If true, publishing a post requires the player to have a specific item. */
        var requireItemToPost: Boolean = false,

        /** Registry id of the required item when requireItemToPost=true (e.g. "hytale:paper"). */
        var requiredItemIdToPost: String = "",
    )

    data class Messages(
        /** If false, the delete button is hidden and delete actions are ignored. */
        var allowDelete: Boolean = true,

        /** If true, sending messages requires the player to have a specific item. */
        var requireItemToSend: Boolean = false,

        /** Registry id of the required item when requireItemToSend=true (e.g. "hytale:paper"). */
        var requiredItemIdToSend: String = "",
    )

    data class Guilds(
        /** If true, creating a guild requires the player to have a specific item. */
        var requireItemToCreate: Boolean = false,

        /** Registry id of the required item when requireItemToCreate=true (e.g. "hytale:paper"). */
        var requiredItemIdToCreate: String = "",
    )

    data class GuildBoard(
        /** If false, the guild board post UI is hidden and send actions are ignored. */
        var allowPost: Boolean = true,

        /** If true, posting on guild board requires the player to have a specific item. */
        var requireItemToPost: Boolean = false,

        /** Registry id of the required item when requireItemToPost=true (e.g. "hytale:book"). */
        var requiredItemIdToPost: String = "",
    )
}
