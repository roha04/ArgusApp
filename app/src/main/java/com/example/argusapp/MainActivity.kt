package com.example.argusapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.ui.admin.AdminMainActivity
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.citizen.CitizenMainActivity
import com.example.argusapp.ui.police.PoliceMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        } else {
            // No user is signed in, redirect to login
            navigateToLogin()
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