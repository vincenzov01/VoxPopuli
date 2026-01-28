package com.voxpopuli.services

import com.voxpopuli.data.Guild
import com.voxpopuli.data.GuildMember
import java.util.UUID

object GuildServices {
    fun getGuildForPlayer(playerUuid: UUID): Guild? {
        // Per ora simuliamo una gilda se l'ID corrisponde al tuo (test)
        // Altrimenti ritorna null e l'HTML mostrerà "Non fai parte di una gilda"
        return null
    }
}