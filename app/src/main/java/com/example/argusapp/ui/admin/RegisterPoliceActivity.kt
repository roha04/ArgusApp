package com.example.argusapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.databinding.ActivityRegisterPoliceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class RegisterPoliceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPoliceBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var adminUser: FirebaseUser? = null

    companion object {
        private const val TAG = "RegisterPoliceActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPoliceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Store reference to current admin user
        adminUser = auth.currentUser

        if (adminUser == null) {
            Toast.makeText(this, "Адміністратор не авторизований", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.buttonRegister.setOnClickListener {
            if (validateInputs()) {
                registerPoliceOfficer()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (name.isEmpty()) {
            binding.editTextName.error = "Введіть ім'я"
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Введіть коректну електронну пошту"
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.editTextPassword.error = "Пароль повинен містити не менше 6 символів"
            return false
        }

        return true
    }

//    private fun registerPoliceOfficer() {
//        val name = binding.editTextName.text.toString().trim()
//        val email = binding.editTextEmail.text.toString().trim()
//        val password = binding.editTextPassword.text.toString().trim()
//        val badgeNumber = binding.editTextBadgeNumber?.text?.toString()?.trim() ?: ""
//        val department = binding.editTextDepartment?.text?.toString()?.trim() ?: ""
//
//        setLoading(true)
//
//        // Step 1: Store current admin credentials
//        val adminUid = auth.currentUser?.uid
//
//        if (adminUid == null) {
//            setLoading(false)
//            Toast.makeText(this, "Адміністратор не авторизований", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        // Step 2: Create new police user document FIRST while still authenticated as admin
//        val newPoliceId = db.collection("users").document().id // Generate a new document ID
//
//        val userMap = hashMapOf(
//            "displayName" to name,
//            "email" to email,
//            "role" to "police",
//            "createdAt" to com.google.firebase.Timestamp.now()
//        )
//
//        if (badgeNumber.isNotEmpty()) userMap["badgeNumber"] = badgeNumber
//        if (department.isNotEmpty()) userMap["department"] = department
//
//        // Write to Firestore AS ADMIN (because we're still logged in as admin)
//        db.collection("users").document(newPoliceId)
//            .set(userMap)
//            .addOnSuccessListener {
//                Log.d("RegisterPolice", "Created Firestore document for police")
//
//                // Step 3: Now create the authentication account with the SAME ID
//                auth.createUserWithEmailAndPassword(email, password)
//                    .addOnSuccessListener { authResult ->
//                        // Update the auth UID if needed
//                        val authUser = authResult.user
//
//                        // Successfully created both document and auth user
//                        setLoading(false)
//                        Toast.makeText(
//                            this,
//                            "Поліцейський $name успішно зареєстрований",
//                            Toast.LENGTH_LONG
//                        ).show()
//
//                        // Go back to admin screen
//                        finish()
//                    }
//                    .addOnFailureListener { e ->
//                        // Failed to create auth user, should delete the Firestore document
//                        db.collection("users").document(newPoliceId).delete()
//
//                        setLoading(false)
//                        Toast.makeText(
//                            this,
//                            "Помилка створення авторизації: ${e.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//            }
//            .addOnFailureListener { e ->
//                setLoading(false)
//                Toast.makeText(
//                    this,
//                    "Помилка створення профілю: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//    }
private fun registerPoliceOfficer() {
    val name = binding.editTextName.text.toString().trim()
    val email = binding.editTextEmail.text.toString().trim()
    val password = binding.editTextPassword.text.toString().trim()
    val badgeNumber = binding.editTextBadgeNumber?.text?.toString()?.trim() ?: ""
    val department = binding.editTextDepartment?.text?.toString()?.trim() ?: ""

    setLoading(true)

    // FIXED APPROACH: Create Auth user first, then use its UID for Firestore
    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { authResult ->
            val newUser = authResult.user

            if (newUser != null) {
                // Use the Auth UID for the Firestore document
                val userMap = hashMapOf(
                    "displayName" to name,
                    "email" to email,
                    "role" to "police",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                if (badgeNumber.isNotEmpty()) userMap["badgeNumber"] = badgeNumber
                if (department.isNotEmpty()) userMap["department"] = department

                // Create Firestore document with the SAME UID as the auth user
                db.collection("users").document(newUser.uid)
                    .set(userMap)
                    .addOnSuccessListener {
                        // Successfully created both
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Поліцейський $name успішно зареєстрований",
                            Toast.LENGTH_LONG
                        ).show()

                        // Re-authenticate as admin before leaving
                        auth.signOut()

                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Помилка створення профілю: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
        .addOnFailureListener { e ->
            setLoading(false)
            Toast.makeText(
                this,
                "Помилка створення авторизації: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
}


    private fun setLoading(isLoading: Boolean) {
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
    }
}