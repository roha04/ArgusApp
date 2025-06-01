package com.example.argusapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.ui.admin.AdminMainActivity
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.citizen.CitizenMainActivity
import com.example.argusapp.ui.police.PoliceMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase auth and database
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is already authenticated
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is already signed in, check their role
            checkUserRoleAndRedirect(currentUser.uid)

            // Register for push notifications
            registerForPushNotifications(currentUser.uid)
        } else {
            // No user is signed in, redirect to login
            navigateToLogin()
        }

        // Handle notification data if the app was launched from a notification
        handleNotificationData(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification data if the app was already running
        handleNotificationData(intent)
    }

    private fun handleNotificationData(intent: Intent) {
        // Check if the app was launched from a notification
        if (intent.extras != null) {
            val messageId = intent.getStringExtra("messageId")
            val type = intent.getStringExtra("type")

            if (type == "mass_message" && messageId != null) {
                Log.d(TAG, "App opened from notification. Message ID: $messageId")

                // You can add specific handling for when the app is opened from a notification
                // For example, you might want to navigate to a specific screen
            }
        }
    }

    private fun registerForPushNotifications(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Save the token to Firestore
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error updating FCM token", e)
                }

            // Get user role and subscribe to appropriate topics
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role") ?: "citizen"

                        // Subscribe to all_users topic
                        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "Subscribed to all_users topic")
                                } else {
                                    Log.e(TAG, "Failed to subscribe to all_users topic", task.exception)
                                }
                            }

                        // Subscribe to role-specific topic
                        FirebaseMessaging.getInstance().subscribeToTopic("user_type_$role")
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "Subscribed to user_type_$role topic")
                                } else {
                                    Log.e(TAG, "Failed to subscribe to user_type_$role topic", task.exception)
                                }
                            }

                        // Subscribe to additional topics if needed
                        when (role) {
                            "admin" -> {
                                FirebaseMessaging.getInstance().subscribeToTopic("admin_alerts")
                            }
                            "police" -> {
                                FirebaseMessaging.getInstance().subscribeToTopic("emergency_alerts")
                            }
                        }
                    }
                }
        }
    }

    private fun checkUserRoleAndRedirect(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "citizen"

                    val intent = when (role) {
                        "admin" -> Intent(this, AdminMainActivity::class.java)
                        "police" -> Intent(this, PoliceMainActivity::class.java)
                        else -> Intent(this, CitizenMainActivity::class.java)
                    }

                    startActivity(intent)
                    finish()
                } else {
                    // User profile not found in database
                    auth.signOut()
                    navigateToLogin()
                }
            }
            .addOnFailureListener {
                // Error accessing Firestore
                auth.signOut()
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so user can't go back to it
    }
}