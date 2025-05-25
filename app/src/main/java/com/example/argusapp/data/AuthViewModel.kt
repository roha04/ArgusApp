package com.example.argusapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        // Перевіряємо, чи користувач вже увійшов
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRole(currentUser.uid)
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    checkUserRole(user.uid)
                } else {
                    _authState.value = AuthState.Error("Помилка автентифікації")
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Помилка входу")
            }
    }

    fun register(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Створюємо профіль користувача в Firestore
                    val userMap = hashMapOf(
                        "displayName" to name,
                        "email" to email,
                        "role" to "citizen", // За замовчуванням - громадянин
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("users").document(user.uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated(UserRole.CITIZEN)
                        }
                        .addOnFailureListener { e ->
                            _authState.value =
                                AuthState.Error(e.localizedMessage ?: "Помилка створення профілю")
                        }
                } else {
                    _authState.value = AuthState.Error("Помилка реєстрації")
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Помилка реєстрації")
            }
    }

    private fun checkUserRole(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "citizen"
                    val userRole = when (role) {
                        "police" -> UserRole.POLICE
                        "admin" -> UserRole.ADMIN
                        else -> UserRole.CITIZEN
                    }
                    _authState.value = AuthState.Authenticated(userRole)
                } else {
                    _authState.value = AuthState.Error("Профіль користувача не знайдено")
                }
            }
            .addOnFailureListener { e ->
                _authState.value =
                    AuthState.Error(e.localizedMessage ?: "Помилка отримання профілю")
            }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.NotAuthenticated
    }
}

sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val role: UserRole) : AuthState()
    data class Error(val message: String) : AuthState()
}

enum class UserRole {
    CITIZEN, POLICE, ADMIN
}