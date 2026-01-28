package com.voxpopuli.ui

import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import au.ellie.hyui.builders.PageBuilder
import au.ellie.hyui.html.TemplateProcessor
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.voxpopuli.services.PostServices
import com.voxpopuli.services.GuildServices
import com.voxpopuli.services.MessageServices

object VoxMainPage {

    fun open(playerRef: PlayerRef, store: Store<EntityStore>) {
        // 1. RECUPERO DATI DAI SERVIZI
        val posts = PostServices.getAllPosts()
        val guild = GuildServices.getGuildForPlayer(playerRef.uuid)
        val messages = MessageServices.getMessagesForPlayer(playerRef.uuid)

        // 2. CONFIGURAZIONE TEMPLATE
        val template = TemplateProcessor()
            .setVariable("playerName", playerRef.username)

            // Dati Cronache (Posts)
            .setVariable("posts", posts.map { post ->
                mapOf<String, Any>(
                    "id" to post.id,
                    "authorName" to post.authorName,
                    "category" to post.category.name,
                    "content" to post.content,
                    "timestamp" to post.formattedTime,
                    "likeCount" to post.likes,
                    "commentCount" to post.comments.size
                )
            })

            // Dati Gilda
            .setVariable("hasGuild", guild != null)
            .apply {
                if (guild != null) {
                    setVariable("guildName", guild.name)
                    setVariable("guildTag", guild.tag)
                    setVariable("guildMotto", guild.motto)
                    setVariable("guildLevel", guild.level)
                    setVariable("memberCount", guild.members.size)
                    setVariable("playerRank", guild.getRankOf(playerRef.uuid))
                    setVariable("members", guild.members.map {
                        mapOf("name" to it.username, "rank" to it.rankName)
                    })
                }
            }

            // Dati Messaggi
            .setVariable("messages", messages.map { msg ->
                mapOf<String, Any>(
                    "id" to msg.id,
                    "senderName" to msg.senderName,
                    "content" to msg.content,
                    // Ora msg.formattedTime è risolto!
                    "timestamp" to msg.formattedTime,
                    "isUnread" to !msg.read
                )
            })

        // 3. COSTRUZIONE PAGINA
        val builder = PageBuilder.pageForPlayer(playerRef)
            .loadHtml("Pages/VoxMainPage.html", template)

        // --- GESTIONE EVENTI ---

        // Creazione Post
        builder.addEventListener("btn-create-post", CustomUIEventBindingType.Activating) { _, event ->
            val content = event.getValue("new-post-content")?.toString() ?: ""
            val category = event.getValue("post-category")?.toString() ?: "GENERAL"

            if (content.isNotBlank()) {
                PostServices.createPost(playerRef, content, category)
                open(playerRef, store) // Refresh
            }
        }

        // Like ai Post (Esempio dinamico)
        posts.forEach { post ->
            builder.addEventListener("btn-like-${post.id}", CustomUIEventBindingType.Activating) { _, _ ->
                PostServices.addLike(post.id, playerRef.uuid)
                open(playerRef, store)
            }
        }

        builder.open(store)
    }
}