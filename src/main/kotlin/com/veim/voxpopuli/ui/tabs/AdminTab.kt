package com.veim.voxpopuli.ui.tabs

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage

interface AdminTab {
    val tabId: String

    fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    )

    /**
     * Gestisce un evento.
     * @return Una stringa di stato da mostrare (es. "Salvato"), oppure null se l'evento non è stato gestito o non richiede feedback.
     */
    fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String?
}
