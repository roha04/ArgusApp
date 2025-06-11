package com.example.argusapp.ui.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.argusapp.R
import com.example.argusapp.databinding.ActivityRegisterBinding
import com.example.argusapp.ui.citizen.CitizenMainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Run entrance animations
        setupEntranceAnimations()

        // Set up click listeners
        setupClickListeners()
    }

    /**
     * Sets up entrance animations for logo, title, and registration card
     */
    private fun setupEntranceAnimations() {
        val logo = binding.appLogo
        val title = binding.textViewTitle
        val card = binding.registrationCard

        // Initially set alpha to 0
        logo.alpha = 0f
        title.alpha = 0f
        card.alpha = 0f
        card.translationY = 100f

        // Logo animation
        val logoAnim = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Title text animation
        val titleAnim = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Card animations
        val cardAlphaAnim = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val cardTransAnim = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        // Play animations sequentially
        AnimatorSet().apply {
            play(logoAnim)
            play(titleAnim).after(150)
            play(cardAlphaAnim).after(300)
            play(cardTransAnim).with(cardAlphaAnim)
            start()
        }
    }

    /**
     * Set up click listeners for buttons and text views
     */
    private fun setupClickListeners() {
        // Register button click listener
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                registerCitizen(name, email, password)
            }
        }


    // Replace the existing login text click listener in setupClickListeners()
        binding.textViewLogin.setOnClickListener {
            // Apply finish with transition animation
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    // Also add this method to RegisterActivity to handle back button press
    override fun onBackPressed() {
        super.onBackPressed()
        // Apply same transition animation when using back button
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Validates user input fields
     */
    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.textInputLayoutName.error = "Введіть ім'я"
            isValid = false
        } else {
            binding.textInputLayoutName.error = null
        }

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Введіть email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.error = "Введіть коректний email"
            isValid = false
        } else {
            binding.textInputLayoutEmail.error = null
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Введіть пароль"
            isValid = false
        } else if (password.length < 6) {
            binding.textInputLayoutPassword.error = "Пароль має бути не менше 6 символів"
            isValid = false
        } else {
            binding.textInputLayoutPassword.error = null
        }

        if (confirmPassword != password) {
            binding.textInputLayoutConfirmPassword.error = "Паролі не співпадають"
            isValid = false
        } else {
            binding.textInputLayoutConfirmPassword.error = null
        }

        return isValid
    }

    /**
     * Registers a new citizen user with Firebase Auth and creates profile document
     */
    private fun registerCitizen(name: String, email: String, password: String) {
        showProgressBar(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Create user document in Firestore
                        val userMap = hashMapOf(
                            "displayName" to name,
                            "email" to email,
                            "role" to "citizen",  // Explicitly set role to "citizen"
                            "createdAt" to FieldValue.serverTimestamp(),
                            "photoUrl" to "", // Empty value for profile photo
                            "phone" to ""  // Empty value for phone
                        )

                        db.collection("users").document(user.uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                showProgressBar(false)
                                // Show success message
                                Snackbar.make(
                                    binding.registerRoot,
                                    "Реєстрація успішна!",
                                    Snackbar.LENGTH_SHORT
                                ).show()

                                // Delay transition slightly to allow user to see success message
                                binding.registerRoot.postDelayed({
                                    startActivity(Intent(this, CitizenMainActivity::class.java))
                                    finishAffinity() // Close all previous screens
                                }, 800)
                            }
                            .addOnFailureListener { e ->
                                showProgressBar(false)
                                showError("Помилка створення профілю: ${e.message}")
                                auth.currentUser?.delete() // Delete user if profile creation failed
                            }
                    }
                } else {
                    showProgressBar(false)
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Цей email вже зареєстрований"
                        is FirebaseAuthWeakPasswordException -> "Пароль занадто слабкий"
                        else -> "Помилка реєстрації: ${task.exception?.message}"
                    }
                    showError(errorMessage)
                }
            }
    }

    /**
     * Shows/hides progress bar with smooth animation
     */
    private fun showProgressBar(show: Boolean) {
        val registerButton = binding.buttonRegister
        val progressBar = binding.progressBar

        if (show) {
            // Animate button out, progress in
            registerButton.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    registerButton.isEnabled = false
                    registerButton.isVisible = false
                    progressBar.isVisible = true
                    progressBar.alpha = 0f
                    progressBar.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }.start()
        } else {
            // Animate progress out, button in
            progressBar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    progressBar.isVisible = false
                    registerButton.isEnabled = true
                    registerButton.isVisible = true
                    registerButton.alpha = 0f
                    registerButton.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }.start()
        }
    }

    /**
     * Shows error message with Snackbar
     */
    private fun showError(message: String) {
        Snackbar.make(
            binding.registerRoot,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
