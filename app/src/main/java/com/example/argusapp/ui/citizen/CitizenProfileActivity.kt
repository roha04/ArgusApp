package com.example.argusapp.ui.citizen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ActivityCitizenProfileBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CitizenProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var currentUser: User? = null
    private var selectedImageUri: Uri? = null

    // Результати запусків активностей для вибору зображення
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.data != null) {
                selectedImageUri = data.data
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(binding.imageViewProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitizenProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштування Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        loadUserProfile()
        setupListeners()
    }

    private fun loadUserProfile() {
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Помилка аутентифікації")
            redirectToLogin()
            return
        }

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    currentUser?.id = document.id
                    displayUserProfile()
                } else {
                    showToast("Профіль користувача не знайдено")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Помилка завантаження профілю: ${e.message}")
                finish()
            }
    }

    private fun displayUserProfile() {
        val user = currentUser ?: return
        val firebaseUser = auth.currentUser ?: return

        // Відображення імені
        binding.editTextName.setText(user.displayName)

        // Відображення Email
        binding.textViewEmail.text = firebaseUser.email

        // Відображення телефону
        binding.editTextPhone.setText(user.phone)

        // Завантаження фото профілю
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        }
    }

    private fun setupListeners() {
        // Вибір фото профілю
        binding.buttonChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Зміна пароля
        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Збереження змін у профілі
        binding.buttonSaveProfile.setOnClickListener {
            if (validateForm()) {
                saveProfile()
            }
        }

        // Вихід з облікового запису
        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Вихід")
                .setMessage("Ви дійсно хочете вийти з облікового запису?")
                .setPositiveButton("Так") { _, _ ->
                    auth.signOut()
                    redirectToLogin()
                }
                .setNegativeButton("Ні", null)
                .show()
        }

        // Видалення облікового запису
        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Перевірка імені
        val displayName = binding.editTextName.text.toString().trim()
        if (displayName.isEmpty()) {
            binding.textInputLayoutName.error = "Введіть ім'я"
            isValid = false
        } else {
            binding.textInputLayoutName.error = null
        }

        return isValid
    }

    private fun saveProfile() {
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            showToast("Помилка аутентифікації")
            return
        }

        val displayName = binding.editTextName.text.toString().trim()
        val phoneNumber = binding.editTextPhone.text.toString().trim()

        // Якщо вибрано нове зображення, спочатку завантажуємо його
        if (selectedImageUri != null) {
            uploadProfileImage(selectedImageUri!!) { photoUrl ->
                updateUserProfile(userId, displayName, phoneNumber, photoUrl)
            }
        } else {
            updateUserProfile(userId, displayName, phoneNumber, currentUser?.photoUrl ?: "")
        }
    }

    private fun uploadProfileImage(imageUri: Uri, onComplete: (String) -> Unit) {
        val imageName = "profile_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child("profile_images/$imageName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onComplete(downloadUrl.toString())
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Помилка завантаження фото: ${e.message}")
            }
    }

    private fun updateUserProfile(
        userId: String,
        displayName: String,
        phoneNumber: String,
        photoUrl: String
    ) {
        val userUpdates = hashMapOf<String, Any>(
            "displayName" to displayName,
            "phoneNumber" to phoneNumber,
            "photoUrl" to photoUrl,
            "updatedAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .update(userUpdates)
            .addOnSuccessListener {
                // Оновлення інформації в Auth
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .apply {
                        if (photoUrl.isNotEmpty()) {
                            setPhotoUri(Uri.parse(photoUrl))
                        }
                    }
                    .build()

                auth.currentUser?.updateProfile(profileUpdates)
                    ?.addOnSuccessListener {
                        showLoading(false)
                        showToast("Профіль успішно оновлено")
                    }
                    ?.addOnFailureListener { e ->
                        showLoading(false)
                        showToast("Помилка оновлення профілю в Auth: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Помилка оновлення профілю: ${e.message}")
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Зміна пароля")
            .setView(dialogView)
            .setPositiveButton("Змінити", null)
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()

        // Налаштування обробників кнопок після відображення діалогу
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val currentPassword =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextCurrentPassword).text.toString()
            val newPassword =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNewPassword).text.toString()
            val confirmPassword =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextConfirmPassword).text.toString()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Заповніть всі поля")
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                showToast("Нові паролі не співпадають")
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                showToast("Новий пароль має містити не менше 6 символів")
                return@setOnClickListener
            }

            changePassword(currentPassword, newPassword, dialog)
        }
    }

    private fun changePassword(
        currentPassword: String,
        newPassword: String,
        dialog: AlertDialog
    ) {
        showLoading(true)

        val user = auth.currentUser
        if (user == null || user.email == null) {
            showLoading(false)
            showToast("Помилка аутентифікації")
            return
        }

        // Перевірка поточного пароля
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Зміна пароля
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        showLoading(false)
                        dialog.dismiss()
                        showToast("Пароль успішно змінено")
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        showToast("Помилка зміни пароля: ${e.message}")
                    }
            }
            .addOnFailureListener {
                showLoading(false)
                showToast("Поточний пароль невірний")
            }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Видалення облікового запису")
            .setMessage("Ви дійсно хочете видалити свій обліковий запис? Це дія незворотна і всі ваші дані буде видалено.")
            .setPositiveButton("Видалити") { _, _ ->
                showPasswordConfirmationDialog()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showPasswordConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_password, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Підтвердження")
            .setMessage("Для видалення облікового запису введіть свій пароль")
            .setView(dialogView)
            .setPositiveButton("Підтвердити", null)
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextConfirmPassword).text.toString()
            if (password.isEmpty()) {
                showToast("Введіть пароль")
                return@setOnClickListener
            }

            deleteAccount(password, dialog)
        }
    }

    private fun deleteAccount(password: String, dialog: AlertDialog) {
        showLoading(true)

        val user = auth.currentUser
        if (user == null || user.email == null) {
            showLoading(false)
            showToast("Помилка аутентифікації")
            return
        }

        // Перевірка пароля
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Видалення даних користувача з Firestore
                db.collection("users").document(user.uid)
                    .delete()
                    .addOnSuccessListener {
                        // Видалення облікового запису
                        user.delete()
                            .addOnSuccessListener {
                                showLoading(false)
                                dialog.dismiss()
                                showToast("Обліковий запис успішно видалено")
                                redirectToLogin()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                showToast("Помилка видалення облікового запису: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        showToast("Помилка видалення даних користувача: ${e.message}")
                    }
            }
            .addOnFailureListener {
                showLoading(false)
                showToast("Пароль невірний")
            }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}