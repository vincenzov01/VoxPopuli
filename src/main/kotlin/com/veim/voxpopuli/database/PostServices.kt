package com.veim.voxpopuli.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object PostServices {
    fun createPost(authorId: Int, content: String, timestamp: Long = System.currentTimeMillis()): Post? {
        var postId: Int? = null
        transaction {
            postId = Posts.insertAndGetId { row ->
                row[Posts.authorId] = authorId
                row[Posts.content] = content
                row[Posts.timestamp] = timestamp
            }?.value
        }
        return postId?.let { getPostById(it) }
    }

    fun getPostById(id: Int): Post? = transaction {
        Posts.select { Posts.id eq id }.mapNotNull { postRow ->
            val postId = postRow[Posts.id].value
            val likedBy = PostLikes.select { PostLikes.postId eq postId }.map { it[PostLikes.userId] }.toSet()
            Post(
                id = postId,
                authorId = postRow[Posts.authorId],
                content = postRow[Posts.content],
                timestamp = postRow[Posts.timestamp],
                likedBy = likedBy
            )
        }.singleOrNull()
    }

    fun getPostsByAuthor(authorId: Int): List<Post> = transaction {
        Posts.select { Posts.authorId eq authorId }.map { postRow ->
            val postId = postRow[Posts.id].value
            val likedBy = PostLikes.select { PostLikes.postId eq postId }.map { it[PostLikes.userId] }.toSet()
            Post(
                id = postId,
                authorId = postRow[Posts.authorId],
                content = postRow[Posts.content],
                timestamp = postRow[Posts.timestamp],
                likedBy = likedBy
            )
        }
    }

    fun updatePost(post: Post): Boolean = transaction {
        Posts.update({ Posts.id eq post.id }) { row ->
            row[Posts.content] = post.content
            row[Posts.timestamp] = post.timestamp
        } > 0
    }

    fun deletePost(id: Int): Boolean = transaction {
        Posts.deleteWhere { Posts.id eq id } > 0
    }

    fun getAllPosts(): List<Post> = transaction {
        Posts.selectAll().map { postRow ->
            val postId = postRow[Posts.id].value
            val likedBy = PostLikes.select { PostLikes.postId eq postId }.map { it[PostLikes.userId] }.toSet()
            Post(
                id = postId,
                authorId = postRow[Posts.authorId],
                content = postRow[Posts.content],
                timestamp = postRow[Posts.timestamp],
                likedBy = likedBy
            )
        }
    }

    // --- LIKE MANAGEMENT ---
    fun addLike(postId: Int, userId: Int): Boolean = transaction {
        val alreadyLiked = PostLikes
            .select { (PostLikes.postId eq postId) and (PostLikes.userId eq userId) }
            .count() > 0
        if (alreadyLiked) return@transaction false

        PostLikes.insert {
            it[PostLikes.postId] = postId
            it[PostLikes.userId] = userId
        }
        true
    }

    fun removeLike(postId: Int, userId: Int): Boolean = transaction {
        PostLikes.deleteWhere { (PostLikes.postId eq postId) and (PostLikes.userId eq userId) } > 0
    }

    fun hasUserLiked(postId: Int, userId: Int): Boolean = transaction {
        PostLikes.select { (PostLikes.postId eq postId) and (PostLikes.userId eq userId) }.count() > 0
    }

    fun countLikes(postId: Int): Int = transaction {
        PostLikes.select { PostLikes.postId eq postId }.count().toInt()
    }

    fun getUsersWhoLiked(postId: Int): Set<Int> = transaction {
        PostLikes.select { PostLikes.postId eq postId }.map { it[PostLikes.userId] }.toSet()
    }

}
