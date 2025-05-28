package com.example.argusapp.ui.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.argusapp.R
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.databinding.ActivityLoginBinding
import com.example.argusapp.ui.admin.AdminMainActivity
import com.example.argusapp.ui.citizen.CitizenMainActivity
import com.example.argusapp.ui.police.PoliceMainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
     * Sets up entrance animations for logo, welcome text, and login card
     */
    private fun setupEntranceAnimations() {
        val logo = binding.appLogo
        val welcomeText = binding.textViewWelcome
        val loginCard = binding.loginCard

        // Initially set alpha to 0
        logo.alpha = 0f
        welcomeText.alpha = 0f
        loginCard.alpha = 0f
        loginCard.translationY = 100f

        // Logo animation
        val logoAnim = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Welcome text animation
        val welcomeAnim = ObjectAnimator.ofFloat(welcomeText, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Card animations
        val cardAlphaAnim = ObjectAnimator.ofFloat(loginCard, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val cardTransAnim = ObjectAnimator.ofFloat(loginCard, "translationY", 100f, 0f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
        }

        // Play animations sequentially
        AnimatorSet().apply {
            play(logoAnim)
            play(welcomeAnim).after(150)
            play(cardAlphaAnim).after(300)
            play(cardTransAnim).with(cardAlphaAnim)
            start()
        }
    }

    /**
     * Sets up all button and text view click listeners
     */
    private fun setupClickListeners() {
        // Login button click listener
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        // Register text click listener
        binding.textViewRegister.setOnClickListener {
            // Navigate to registration - available only for citizens
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)

            // Optional: Add transition animation
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Forgot password click listener
        binding.textViewForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    /**
     * Validates user input for email and password
     */
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Введіть email"
            isValid = false
        } else {
            binding.textInputLayoutEmail.error = null
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Введіть пароль"
            isValid = false
        } else {
            binding.textInputLayoutPassword.error = null
        }

        return isValid
    }

    /**
     * Authenticates user with Firebase
     */
    private fun loginUser(email: String, password: String) {
        showProgressBar(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Pass both userId and email to the function
                        checkUserRoleAndRedirect(user.uid, email)
                    } else {
                        showProgressBar(false)
                        showError("Помилка автентифікації")
                    }
                } else {
                    showProgressBar(false)
                    showError("Невірний email або пароль")
                }
            }
    }


    /**
     * Checks user role in Firestore and redirects to appropriate activity
     */
//    private fun checkUserRoleAndRedirect(userId: String) {
//        db.collection("users").document(userId).get()
//            .addOnSuccessListener { document ->
//                showProgressBar(false)
//
//                if (document != null && document.exists()) {
//                    val role = document.getString("role") ?: "citizen"
//
//                    // Create intent based on user role
//                    val intent = when (role) {
//                        "admin" -> Intent(this, AdminMainActivity::class.java)
//                        "police" -> Intent(this, PoliceMainActivity::class.java)
//                        else -> Intent(this, CitizenMainActivity::class.java)
//                    }
//
//                    // Add flags to clear back stack
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//
//                    startActivity(intent)
//
//                    // Optional: Add transition animation
//                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
//
//                    finish()
//                } else {
//                    // Document doesn't exist
//                    showError("Профіль користувача не знайдено")
//                    auth.signOut()
//                }
//            }
//            .addOnFailureListener { e ->
//                showProgressBar(false)
//                showError("Помилка: ${e.localizedMessage}")
//            }
//    }
    // ui/auth/LoginActivity.kt
// Update your checkUserRoleAndRedirect function

    private fun checkUserRoleAndRedirect(userId: String, email: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                showProgressBar(false)

                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "citizen"

                    // Log the login activity
                    ActivityLog.logActivity(
                        userId = userId,
                        userEmail = email,
                        userType = role,
                        action = "Вхід в систему"
                    )

                    // Create intent based on user role
                    val intent = when (role) {
                        "admin" -> Intent(this, AdminMainActivity::class.java)
                        "police" -> Intent(this, PoliceMainActivity::class.java)
                        else -> Intent(this, CitizenMainActivity::class.java)
                    }

                    // Add flags to clear back stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    // Optional: Add transition animation
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

                    finish()
                } else {
                    // Document doesn't exist
                    showError("Профіль користувача не знайдено")
                    auth.signOut()
                }
            }
            .addOnFailureListener { e ->
                showProgressBar(false)
                showError("Помилка: ${e.localizedMessage}")
            }
    }

    /**
     * Shows/hides progress bar with smooth animation
     */
    private fun showProgressBar(show: Boolean) {
        val loginButton = binding.buttonLogin
        val progressBar = binding.progressBar

        if (show) {
            // Animate button out, progress in
            loginButton.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    loginButton.isEnabled = false
                    loginButton.isVisible = false
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
                    loginButton.isEnabled = true
                    loginButton.isVisible = true
                    loginButton.alpha = 0f
                    loginButton.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }.start()
        }
    }

    /**
     * Shows error toast message
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows enhanced forgot password dialog with rounded corners
     */
    private fun showForgotPasswordDialog() {
        // Inflate custom dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextEmailReset)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutResetEmail)
        val progressIndicator =
            dialogView.findViewById<LinearProgressIndicator>(R.id.progressIndicator)

        // Create dialog with no title to use custom background
        val dialog = AlertDialog.Builder(this)
            .setTitle("Відновлення паролю")
            .setView(dialogView)
            .create()

        // Set custom background with rounded corners
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)

        // Show dialog
        dialog.show()

        // Add buttons directly to the AlertDialog
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        // Set positive button (send)
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Надіслати") { _, _ ->
            // This won't be executed as we override its behavior
        }

        // Set negative button (cancel)
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Скасувати") { _, _ ->
            dialog.dismiss()
        }

        // Override positive button click listener to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = emailEditText.text.toString().trim()

            // Validate email
            if (email.isEmpty()) {
                emailLayout.error = "Введіть email для відновлення"
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Введіть коректний email"
            } else {
                emailLayout.error = null

                // Show progress and disable buttons
                progressIndicator.visibility = View.VISIBLE
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false

                // Attempt password reset
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        // Close dialog
                        dialog.dismiss()

                        // Show success message with Snackbar
                        Snackbar.make(
                            binding.loginRoot,
                            "Інструкції з відновлення паролю надіслані на вашу електронну пошту",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        // Re-enable buttons and hide progress
                        progressIndicator.visibility = View.GONE
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false

                        // Show error in dialog
                        val errorMessage = when {
                            e.message?.contains("user-not-found") == true -> "Email не зареєстрований в системі"
                            e.message?.contains("network") == true -> "Перевірте підключення до інтернету"
                            else -> "Помилка: ${e.message}"
                        }

                        emailLayout.error = errorMessage
                    }
            }
        }
    }

}
