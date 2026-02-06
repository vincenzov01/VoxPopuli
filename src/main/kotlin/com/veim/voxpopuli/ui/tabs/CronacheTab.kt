package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.veim.voxpopuli.database.PostServices
import com.veim.voxpopuli.ui.VoxPopuliDashboardPage
import com.veim.voxpopuli.util.InventoryUtil
import java.text.SimpleDateFormat
import java.util.Date

object CronacheTab : BaseVoxTab(id = "cronache", title = "Cronache") {
    override val templatePath: String = "voxpopuli/cronache.ui"

    override fun bindEvents(page: VoxPopuliDashboardPage, evt: UIEventBuilder) {
        // Draft capture (client should include current text for ValueChanged)
        evt.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            page.tabSel("#NewPostInput"),
            page.uiEventData(action = "draft_post"),
            false
        )
		evt.addEventBinding(
			CustomUIEventBindingType.FocusLost,
            page.tabSel("#NewPostInput"),
			page.uiEventData(action = "draft_post"),
			false
		)
		evt.addEventBinding(
			CustomUIEventBindingType.Validating,
            page.tabSel("#NewPostInput"),
			page.uiEventData(action = "draft_post"),
			false
		)
        // Publish
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            page.tabSel("#SendPostButton"),
            page.uiEventData(
                action = "new_post",
                captures = mapOf(
                    "Text" to page.tabSel("#NewPostInput.Value"),
                ),
            ),
            false
        )
    }

    override fun render(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    ) {
        cmd.clear(page.tabSel("#PostList"))

        val posts = PostServices.getAllPosts().sortedByDescending { it.timestamp }
		cmd.set("#ContentTitle.Text", "${title} (${posts.size})")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
        posts.forEachIndexed { index, post ->
            cmd.append(page.tabSel("#PostList"), "voxpopuli/PostItem.ui")
            val selector = page.tabSel("#PostList[${index}]")
            val author = com.veim.voxpopuli.database.UserServices.getUserById(post.authorId)?.username ?: "?"
            cmd.set("$selector #PostAuthor.Text", author)
            cmd.set("$selector #PostTimestamp.Text", dateFormat.format(Date(post.timestamp)))
            cmd.set("$selector #PostContent.Text", post.content)
            cmd.set("$selector #LikeCount.Text", post.likedBy.size.toString())

            evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #LikeButton",
                page.uiEventData(action = "like", postId = post.id),
                false
            )
        }
    }

    override fun handleEvent(
        page: VoxPopuliDashboardPage,
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        data: VoxPopuliDashboardPage.EventData,
        cmd: UICommandBuilder,
        evt: UIEventBuilder
    ): Boolean {
        when (data.action) {
            "draft_post" -> {
                page.draftPostText = data.text
                return true
            }
            "new_post" -> {
                val config = page.configSnapshot.posts
                val postText = (data.text.ifBlank { page.draftPostText }).trim()
				if (postText.isBlank()) {
					// Likely missing FocusLost/Validating capture; keep UI stable.
					return true
				}

                if (config.requireItemToPost) {
                    val requiredItemId = config.requiredItemIdToPost.trim()
                    val hasItem = InventoryUtil.playerHasItemId(store, ref, requiredItemId)
                    if (hasItem != true) return true
                }
                val user = getOrCreateUser(page)
                if (user != null) {
                    PostServices.createPost(user.id, postText)
                    page.draftPostText = ""
                    cmd.set(page.tabSel("#NewPostInput.Value"), "")
                }

                apply(cmd)
                render(page, ref, store, cmd, evt)
                return true
            }
            "like" -> {
                val postId = data.postId
                if (postId < 0) return true
                val user = getOrCreateUser(page)
                if (user != null) {
                    PostServices.addLike(postId, user.id)
                }

                apply(cmd)
                render(page, ref, store, cmd, evt)
                return true
            }
            else -> return false
        }
    }
}
