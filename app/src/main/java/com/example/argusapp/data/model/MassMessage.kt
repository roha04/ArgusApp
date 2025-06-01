// data/model/MassMessage.kt
package com.example.argusapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class MassMessage(
    @DocumentId
    var id: String = "",
    val title: String = "",
    val body: String = "",
    val sentBy: String = "",
    val senderEmail: String = "",
    val sentAt: Timestamp? = null,
    val targetTopic: String = "all",
    val targetUserType: String = "all",
    val deliveryStatus: String = "processing"
)