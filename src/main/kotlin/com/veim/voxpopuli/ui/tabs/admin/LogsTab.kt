package com.veim.voxpopuli.ui.tabs.admin

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.veim.voxpopuli.database.User
import com.veim.voxpopuli.ui.VoxPopuliAdminDashboardPage
import com.veim.voxpopuli.util.FileAuditLog

class LogsTab(
    private val page: VoxPopuliAdminDashboardPage,
) : AdminTab {
    override val tabId: String = VoxPopuliAdminDashboardPage.TAB_LOGS
    private var logsStatus: String = ""
    private var logsText: String = ""

    init {
        // Caricamento iniziale
        refreshLogs()
    }

    private fun refreshLogs() {
        val maxLines = 200
        val lines = FileAuditLog.tailLines(maxLines)
        logsText = if (lines.isEmpty()) "(nessun log)" else lines.joinToString("\n")
        logsStatus = "Mostrati ultimi ${lines.size} eventi"
    }

    override fun render(
        cmd: UICommandBuilder,
        evt: UIEventBuilder,
    ) {
        cmd.set("#LogsStatusLabel.Text", logsStatus)
        cmd.set("#LogsText.Text", logsText)
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshLogsButton", page.uiEventData("refresh_logs"), false)
    }

    override fun handleEvent(
        data: VoxPopuliAdminDashboardPage.EventData,
        user: User,
        isOp: Boolean,
    ): String? =
        if (data.action == "refresh_logs" || (data.action == "tab" && data.tab == tabId)) {
            refreshLogs()
            null // Il refresh UI avviene nel render, non serve messaggio di stato
        } else {
            null
        }
}
