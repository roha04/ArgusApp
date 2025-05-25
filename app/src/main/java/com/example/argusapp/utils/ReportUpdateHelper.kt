package com.example.argusapp.utils

import com.google.firebase.Timestamp
import com.example.argusapp.data.model.ReportUpdate
import com.example.argusapp.data.model.User

object ReportUpdateHelper {

    fun createStatusChangeUpdate(
        reportId: String,
        user: User,
        oldStatus: String,
        newStatus: String
    ): ReportUpdate {
        return ReportUpdate(
            reportId = reportId,
            userId = user.id,
            userDisplayName = user.displayName,
            userRole = user.role,
            actionType = "status_change",
            oldValue = oldStatus,
            newValue = newStatus,
            description = "Статус заявки змінено з '${getStatusText(oldStatus)}' на '${getStatusText(newStatus)}'",
            timestamp = Timestamp.now()
        )
    }

    fun createAssignmentUpdate(
        reportId: String,
        user: User,
        assignedTo: User?
    ): ReportUpdate {
        val oldValue = "unassigned"
        val newValue = assignedTo?.id ?: "unassigned"
        val description = if (assignedTo != null) {
            "Заявку призначено офіцеру ${assignedTo.displayName}"
        } else {
            "Заявку знято з призначення"
        }

        return ReportUpdate(
            reportId = reportId,
            userId = user.id,
            userDisplayName = user.displayName,
            userRole = user.role,
            actionType = "assignment",
            oldValue = oldValue,
            newValue = newValue,
            description = description,
            timestamp = Timestamp.now()
        )
    }

    fun createOfficialNoteUpdate(
        reportId: String,
        user: User,
        note: String
    ): ReportUpdate {
        return ReportUpdate(
            reportId = reportId,
            userId = user.id,
            userDisplayName = user.displayName,
            userRole = user.role,
            actionType = "official_note",
            oldValue = "",
            newValue = "",
            description = "Офіційне повідомлення: $note",
            timestamp = Timestamp.now()
        )
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "new" -> "Новий"
            "in_progress" -> "В обробці"
            "resolved" -> "Вирішено"
            "closed" -> "Закрито"
            else -> status
        }
    }
}