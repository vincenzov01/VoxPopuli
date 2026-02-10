package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.database.PostServices
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.database.UserServices
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.FileAuditLog
import java.text.SimpleDateFormat
import java.util.Date

class PostsTab(
    private val page: VoxPopuliAdminDashboardPage,
) : AdminTab {
    override val tabId: String = VoxPopuliAdminDashboardPage.TAB_POSTS
    private var postsStatus: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    override fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        cmd.clear("#AdminPostList")
        val posts = PostServices.getAllPosts().sortedByDescending { it.timestamp }
        postsStatus = "${posts.size} post"

        posts.forEachIndexed { index, post ->
            cmd.append("#AdminPostList", "voxpopuli/AdminPostItem.ui")
            val selector = "#AdminPostList[$index]"
            val author = UserServices.getUserById(post.authorId)?.username ?: "?"
            cmd.set("$selector #PostAuthor.Text", author)
            cmd.set("$selector #PostTimestamp.Text", dateFormat.format(Date(post.timestamp)))
            cmd.set("$selector #PostContent.Text", post.content)
            cmd.set("$selector #LikeCount.Text", "${post.likedBy.size} like")

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #DeleteButton",
                page.uiEventData(action = "delete_post", id = post.id),
                false,
            )
        }
        cmd.set("#PostsStatusLabel.Text", postsStatus)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshPostsButton", page.uiEventData("refresh_posts"), false)
    }

    override fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String? =
        when (data.action) {
            "refresh_posts" -> {
                "Aggiornato"
            }

            "delete_post" -> {
                val id = data.id
                if (id > 0) {
                    PostServices.deletePost(id)
                    FileAuditLog.logAdminAction(
                        actorUsername = page.player.username,
                        actorUserId = user.id,
                        action = "post.delete",
                        targetPostId = id,
                    )
                    "Post eliminato"
                } else {
                    null
                }
            }

            else -> {
                null
            }
        }
}
