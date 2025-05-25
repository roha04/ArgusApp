package com.example.argusapp.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ReportComment(
    @get:Exclude var id: String = "",
    var reportId: String = "",
    var userId: String = "",
    var userDisplayName: String = "",
    var userRole: String = "",
    var text: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var isOfficial: Boolean = false, // Офіційний коментар від поліції
    var userPhotoUrl: String = ""
) {
    // Помічник для форматування часу
    fun getFormattedTime(): String {
        val date = createdAt.toDate()
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}