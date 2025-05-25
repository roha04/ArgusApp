package com.example.argusapp.data.model


import com.google.firebase.firestore.Exclude

data class Settings(
    @get:Exclude var id: String = "global_settings",
    var enableNotifications: Boolean = true,
    var maintenanceMode: Boolean = false,
    var minAppVersion: String = "1.0.0",
    var termsUrl: String = "",
    var privacyUrl: String = "",
    var supportEmail: String = "support@argusapp.com",
    var aboutText: String = ""
)