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
    var deviceInfo: String = "",
    var ipAddress: String = "",
    var location: String = "",
    var sessionId: String = "",
    var isSuccessful: Boolean = true,
    var resourceType: String = "", // "user", "report", "settings", etc.
    var resourceId: String = ""    // ID of the accessed resource
) {
    companion object {
        // Helper function to log activity directly from the model
        fun logActivity(
            userId: String,
            userEmail: String,
            userType: String,
            action: String,
            details: String = "",
            resourceType: String = "",
            resourceId: String = "",
            isSuccessful: Boolean = true
        ) {
            val deviceInfo = getDeviceInfo()
            val ipAddress = getIPAddress()
            val location = "Unknown" // Will be determined by Cloud Function
            val sessionId = generateSessionId(userId)

            val activityLog = ActivityLog(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userEmail = userEmail,
                userType = translateRole(userType),
                action = action,
                details = details,
                timestamp = Timestamp.now(),
                deviceInfo = deviceInfo,
                ipAddress = ipAddress,
                location = location,
                sessionId = sessionId,
                isSuccessful = isSuccessful,
                resourceType = resourceType,
                resourceId = resourceId
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

        // Get IP address
        private fun getIPAddress(): String {
            return try {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses

                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
                "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        // Generate a session ID
        private fun generateSessionId(userId: String): String {
            val timestamp = System.currentTimeMillis()
            return "$userId-$timestamp"
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