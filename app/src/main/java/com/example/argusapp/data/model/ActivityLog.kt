// data/model/ActivityLog.kt
package com.example.argusapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ActivityLog(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var userType: String = "", // "Адміністратор", "Поліцейський", "Громадянин"
    var action: String = "",
    var details: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var deviceInfo: String = ""
) {
    companion object {
        // Helper function to log activity directly from the model
        fun logActivity(
            userId: String,
            userEmail: String,
            userType: String,
            action: String,
            details: String = ""
        ) {
            val deviceInfo = getDeviceInfo()

            val activityLog = ActivityLog(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userEmail = userEmail,
                userType = translateRole(userType),
                action = action,
                details = details,
                timestamp = Timestamp.now(),
                deviceInfo = deviceInfo
            )

            // Save to Firestore
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("activity_logs").document(activityLog.id)
                .set(activityLog)
                .addOnSuccessListener {
                    android.util.Log.d("ActivityLog", "Activity logged: $action")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ActivityLog", "Error logging activity", e)
                }
        }

        // Get device info for logging
        private fun getDeviceInfo(): String {
            return try {
                val manufacturer = android.os.Build.MANUFACTURER
                val model = android.os.Build.MODEL
                val version = android.os.Build.VERSION.SDK_INT
                val versionRelease = android.os.Build.VERSION.RELEASE

                "$manufacturer $model (Android $versionRelease, API $version)"
            } catch (e: Exception) {
                "Unknown Device"
            }
        }

        // Translate role to human-readable format
        private fun translateRole(role: String): String {
            return when (role.lowercase()) {
                "admin" -> "Адміністратор"
                "police" -> "Поліцейський"
                "citizen" -> "Громадянин"
                else -> role
            }
        }
    }
}