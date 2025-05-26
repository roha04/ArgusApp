package com.example.argusapp.data.model


import android.util.Log
import android.widget.Toast
import com.example.argusapp.utils.ReportUpdateHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.example.argusapp.ui.police.fragments.NewReportsFragment

//data class Report(
//    @get:Exclude var id: String = "",
//    var userId: String = "",
//    var title: String = "",
//    var description: String = "",
//    var status: String = "new", // "new", "in_progress", "resolved", "closed"
//    var createdAt: Timestamp = Timestamp.now(),
//    var updatedAt: Timestamp = Timestamp.now(),
//    var location: GeoPoint? = null,
//    var address: String = "",
//    var category: String = "",
//    var urgency: String = "medium", // "low", "medium", "high"
//    var assignedToId: String = "",
//    var assignedAt: Timestamp? = null,
//    var resolvedAt: Timestamp? = null,
//    var closedAt: Timestamp? = null,
//    var imageUrls: List<String> = emptyList(),
//    var latitude: Double? = null,
//    var longitude: Double? = null,
//
//    // Додаткові поля
//    var userDisplayName: String = "", // Для кешування імені користувача
//    var viewCount: Int = 0,
//    var commentCount: Int = 0,
//
//    // Поля для Firebase Indexing
//    @get:PropertyName("isResolved")
//    @set:PropertyName("isResolved")
//    var isResolved: Boolean = false,
//
//    @get:PropertyName("isAssigned")
//    @set:PropertyName("isAssigned")
//    var isAssigned: Boolean = false
//) {
//    // Функція для отримання статусу в читабельному форматі
//    fun getStatusText(): String {
//        return when (status) {
//            "new" -> "Новий"
//            "in_progress" -> "В обробці"
//            "resolved" -> "Вирішено"
//            "closed" -> "Закрито"
//            else -> "Невідомий"
//        }
//    }
//
//    // Функція для перевірки, чи може користувач редагувати цей звіт
//    fun canEdit(userId: String, userRole: String): Boolean {
//        return this.userId == userId || userRole == "admin" ||
//                (userRole == "police" && this.status != "closed")
//    }
//}
data class Report(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var status: String = "new", // "new", "in_progress", "resolved", "closed"
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),

    // Location data - consolidated
    var location: GeoPoint? = null,
    var address: String = "",
    var locationAccuracy: Float? = null,
    var locationTimestamp: Timestamp? = null,
    var locationSource: String = "", // "gps", "network", "manual", etc.
    var geocodingStatus: String = "", // "success", "failed", "not_attempted"

    var category: String = "",
    var urgency: String = "medium", // "low", "medium", "high"
    var assignedToId: String = "",
    var assignedAt: Timestamp? = null,
    var resolvedAt: Timestamp? = null,
    var closedAt: Timestamp? = null,
    var imageUrls: List<String> = emptyList(),

    // Додаткові поля
    var userDisplayName: String = "", // Для кешування імені користувача
    var viewCount: Int = 0,
    var commentCount: Int = 0,

    // Поля для Firebase Indexing
    @get:PropertyName("isResolved")
    @set:PropertyName("isResolved")
    var isResolved: Boolean = false,

    @get:PropertyName("isAssigned")
    @set:PropertyName("isAssigned")
    var isAssigned: Boolean = false
) {
    val latitude: Double?
        get() = location?.latitude

    val longitude: Double?
        get() = location?.longitude

    // Helper method to update location
    fun updateLocation(lat: Double, lng: Double, accuracy: Float? = null) {
        location = GeoPoint(lat, lng)
        locationAccuracy = accuracy
        locationTimestamp = Timestamp.now()
    }

    // Функція для отримання статусу в читабельному форматі
    fun getStatusText(): String {
        return when (status) {
            "new" -> "Новий"
            "in_progress" -> "В обробці"
            "resolved" -> "Вирішено"
            "closed" -> "Закрито"
            else -> "Невідомий"
        }
    }

    // Функція для перевірки, чи може користувач редагувати цей звіт
    fun canEdit(userId: String, userRole: String): Boolean {
        return this.userId == userId || userRole == "admin" ||
                (userRole == "police" && this.status != "closed")
    }

    // Helper method to check if location data is available
    fun hasLocation(): Boolean {
        return location != null
    }

    // Helper method to check if address data is available
    fun hasAddress(): Boolean {
        return address.isNotEmpty()
    }

}