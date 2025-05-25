package com.example.argusapp.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ReportUpdate(
    @get:Exclude var id: String = "",
    var reportId: String = "",
    var userId: String = "",
    var userDisplayName: String = "",
    var userRole: String = "",
    var actionType: String = "", // "status_change", "assignment", "comment", "resolution"
    var oldValue: String = "",
    var newValue: String = "",
    var description: String = "",
    var timestamp: Timestamp = Timestamp.now()
)