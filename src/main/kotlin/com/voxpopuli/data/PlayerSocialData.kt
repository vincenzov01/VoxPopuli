package com.voxpopuli.data

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import javax.annotation.Nullable

class PlayerSocialData() : Component<EntityStore> {

    var name: String = "PlayerSocialData"
    var chronicleId: String = ""
    var guildId: String = ""
    var friends: MutableList<String> = mutableListOf()
    var followers: MutableList<String> = mutableListOf()
    var following: MutableList<String> = mutableListOf()
    var reputation: Long = 0L

    constructor(clone: PlayerSocialData) : this() {
        this.name = clone.name
        this.chronicleId = clone.chronicleId
        this.guildId = clone.guildId
        this.friends = ArrayList(clone.friends)
        this.followers = ArrayList(clone.followers)
        this.following = ArrayList(clone.following)
        this.reputation = clone.reputation
    }

    companion object {
        val CODEC: BuilderCodec<PlayerSocialData> = BuilderCodec.builder(PlayerSocialData::class.java) { PlayerSocialData() }
            .addField(KeyedCodec("Name", Codec.STRING), { d: PlayerSocialData, v: String -> d.name = v }, { data: PlayerSocialData -> data.name })
            .addField(KeyedCodec("ChronicleId", Codec.STRING), { d: PlayerSocialData, v: String -> d.chronicleId = v }, { data: PlayerSocialData -> data.chronicleId })
            .addField(KeyedCodec("GuildId", Codec.STRING), { d: PlayerSocialData, v: String -> d.guildId = v }, { data: PlayerSocialData -> data.guildId })
            .addField(KeyedCodec("Friends", Codec.STRING_ARRAY), { d: PlayerSocialData, v: Array<String> -> d.friends = v.toMutableList() }, { data: PlayerSocialData -> data.friends.toTypedArray() })
            .addField(KeyedCodec("Followers", Codec.STRING_ARRAY), { d: PlayerSocialData, v: Array<String> -> d.followers = v.toMutableList() }, { data: PlayerSocialData -> data.followers.toTypedArray() })
            .addField(KeyedCodec("Following", Codec.STRING_ARRAY), { d: PlayerSocialData, v: Array<String> -> d.following = v.toMutableList() }, { data: PlayerSocialData -> data.following.toTypedArray() })
            .addField(KeyedCodec("Reputation", Codec.LONG), { d: PlayerSocialData, v: Long -> d.reputation = v }, { data: PlayerSocialData -> data.reputation })
            .build()
    }

    @Nullable
    override fun clone(): Component<EntityStore> = PlayerSocialData(this)
}