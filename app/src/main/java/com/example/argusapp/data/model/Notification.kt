package com.example.argusapp.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Notification(
    @get:Exclude var id: String = "",
    var userId: String = "", // Користувач, якому адресоване повідомлення
    var title: String = "",
    var message: String = "",
    var type: String = "", // "report_update", "comment", "assignment", "status_change"
    var relatedId: String = "", // ID звіту або коментаря
    var createdAt: Timestamp = Timestamp.now(),
    var isRead: Boolean = false
)