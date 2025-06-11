package com.example.argusapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class Department(
    @get:Exclude var id: String = "",
    var name: String = "",
    var address: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var description: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),

    // Location information
    var location: GeoPoint? = null,

    // Coverage area (can be expanded later to include polygon boundaries)
    var jurisdictionRadius: Double = 0.0, // in kilometers

    // Statistics and metrics
    var officerCount: Int = 0,
    var activeReportsCount: Int = 0,
    var completedReportsCount: Int = 0,

    // Status
    var isActive: Boolean = true
) {
    fun getFormattedAddress(): String {
        return address.ifEmpty { "No address specified" }
    }

    fun getOfficerCountText(): String {
        return "$officerCount officers"
    }
}