// data/model/SecurityAlert.kt
package com.example.argusapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class SecurityAlert(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var userType: String = "",
    var alertType: String = "", // "login_anomaly", "excessive_activity", "unusual_access", etc.
    var severity: Int = 0,      // 1-5, with 5 being most severe
    var description: String = "",
    var details: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var relatedLogIds: List<String> = listOf(),
    var status: String = "new", // "new", "investigating", "resolved", "false_positive"
    var resolvedBy: String = "",
    var resolvedAt: Timestamp? = null,
    var notes: String = ""
) {
    fun getSeverityText(): String {
        return when (severity) {
            1 -> "Низька"
            2 -> "Помірна"
            3 -> "Середня"
            4 -> "Висока"
            5 -> "Критична"
            else -> "Невідома"
        }
    }

    fun getStatusText(): String {
        return when (status) {
            "new" -> "Новий"
            "investigating" -> "Розслідується"
            "resolved" -> "Вирішено"
            "false_positive" -> "Помилкове спрацювання"
            else -> "Невідомо"
        }
    }

    fun getAlertTypeText(): String {
        return when (alertType) {
            "login_anomaly" -> "Аномалія входу"
            "excessive_activity" -> "Надмірна активність"
            "unusual_access" -> "Незвичний доступ"
            "permission_violation" -> "Порушення прав доступу"
            "multiple_failures" -> "Багаторазові невдачі"
            "unusual_time" -> "Незвичний час активності"
            "geolocation_anomaly" -> "Аномалія геолокації"
            else -> alertType
        }
    }
}