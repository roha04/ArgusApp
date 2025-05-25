package com.example.argusapp.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Report(
    @get:Exclude var id: String = "",
    var userId: String = "",
    var title: String = "",
    var description: String = "",
    var status: String = "new", // "new", "in_progress", "resolved", "closed"
    var createdAt: Timestamp = Timestamp.now(),
    var updatedAt: Timestamp = Timestamp.now(),
    var location: GeoPoint? = null,
    var address: String = "",
    var category: String = "",
    var urgency: String = "medium", // "low", "medium", "high"
    var assignedToId: String = "",
    var assignedAt: Timestamp? = null,
    var resolvedAt: Timestamp? = null,
    var closedAt: Timestamp? = null,
    var imageUrls: List<String> = emptyList(),
    var latitude: Double? = null,
    var longitude: Double? = null,

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
}