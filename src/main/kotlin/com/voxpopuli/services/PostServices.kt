package com.voxpopuli.services

import com.hypixel.hytale.server.core.universe.PlayerRef
import com.voxpopuli.data.Post
import com.voxpopuli.data.PostCategory
import com.voxpopuli.managers.DatabaseManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object PostServices {

    // Carica i post dal database invece che da una lista vuota
    fun getAllPosts(): List<Post> {
        return DatabaseManager.getAllPosts()
    }

    fun createPost(player: PlayerRef, content: String, categoryStr: String) {
        val category = try { PostCategory.valueOf(categoryStr) } catch (e: Exception) { PostCategory.GENERAL }

        val newPost = Post(
            id = UUID.randomUUID().toString().substring(0, 8),
            authorName = player.username,
            authorUuid = player.uuid,
            content = content,
            category = category,
            formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        )

        // SALVATAGGIO REALE NEL DB
        DatabaseManager.savePost(newPost)
    }

    fun addLike(postId: String, playerUuid: UUID) {
        // Qui dovresti implementare una funzione updateLikes nel DatabaseManager
        DatabaseManager.updatePostLikes(postId)
    }
}