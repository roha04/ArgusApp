package com.example.argusapp.data.model


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class User(
    @get:Exclude var id: String = "",
    var displayName: String = "",
    var email: String = "",
    var role: String = "citizen", // "admin", "police", "citizen"
    var photoUrl: String = "",
    var phone: String = "",
    var createdAt: Timestamp = Timestamp.now(),

    // Add this property to your User class
    @get:Exclude var isSelected: Boolean = false,
    // Поля для поліцейських
    var badgeNumber: String = "",
    var department: String = "",

    // Додаткові поля
    var lastActive: Timestamp = Timestamp.now(),
    var deviceToken: String = "", // Для push-сповіщень

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true


) {
    // Метод для перевірки ролі
    fun hasRole(requiredRole: String): Boolean {
        return this.role == requiredRole
    }

    // Метод для відображення імені
    fun getFormattedName(): String {
        return when (role) {
            "police" -> "Офіцер $displayName"
            "admin" -> "Адмін $displayName"
            else -> displayName
        }
    }
}