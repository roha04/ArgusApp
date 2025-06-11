package com.example.argusapp.ui.citizen.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.FragmentCitizenProfileBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CitizenProfileFragment : Fragment() {

    private var _binding: FragmentCitizenProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private val PICK_IMAGE_REQUEST = 1001
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitizenProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Load user data
        loadUserData()

        // Setup save button
        binding.buttonSave.setOnClickListener {
            updateProfile()
        }

        // Setup edit mode toggle
        binding.buttonEdit.setOnClickListener {
            toggleEditMode(true)
        }

        // Cancel button
        binding.buttonCancel.setOnClickListener {
            toggleEditMode(false)
            loadUserData() // Reset fields to original data
        }

        // Change profile photo button
        binding.fabChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Change password button
        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logout button
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Delete account button
        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Вийти з облікового запису")
            .setMessage("Ви дійсно бажаєте вийти?")
            .setPositiveButton("Так") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finishAffinity() // Закриває всі активності
            }
            .setNegativeButton("Ні", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data

            // Показати вибране зображення
            Glide.with(this)
                .load(selectedImageUri)
                .circleCrop()
                .into(binding.imageViewProfile)

            // Завантажити зображення у Firebase Storage
            uploadProfileImage()
        }
    }

    private fun uploadProfileImage() {
        if (selectedImageUri == null) return

        val userId = auth.currentUser?.uid ?: return
        val fileExtension = getFileExtension(selectedImageUri!!)
        val fileName = "profile_${UUID.randomUUID()}.$fileExtension"

        // Використовуємо шлях, який відповідає правилам безпеки Firebase Storage
        val storageRef = storage.reference.child("profiles/$userId/$fileName")

        binding.progressBar.visibility = View.VISIBLE
        binding.fabChangePhoto.isEnabled = false

        storageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                // Отримання URL завантаженого зображення
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Оновлення URL фото в Firestore
                    updateProfilePhotoUrl(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("CitizenProfile", "Error uploading image", e)
                Toast.makeText(requireContext(), "Помилка завантаження зображення: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.fabChangePhoto.isEnabled = true
            }
    }

    private fun getFileExtension(uri: Uri): String {
        val contentResolver = requireContext().contentResolver
        val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
    }

    private fun updateProfilePhotoUrl(photoUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("photoUrl", photoUrl)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Фото профілю оновлено", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.fabChangePhoto.isEnabled = true
            }
            .addOnFailureListener { e ->
                Log.e("CitizenProfile", "Error updating photo URL", e)
                Toast.makeText(requireContext(), "Помилка оновлення фото профілю: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.fabChangePhoto.isEnabled = true
            }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: ""

        // Перевіряємо, чи фрагмент все ще прикріплений до активності
        if (!isAdded) return

        binding.progressBar.visibility = View.VISIBLE

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Перевіряємо, чи фрагмент все ще прикріплений і binding не null
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    // Отримуємо дані користувача
                    val displayName = document.getString("displayName") ?: ""
                    val role = document.getString("role") ?: "citizen"
                    val phoneNumber = document.getString("phoneNumber") ?: ""
                    val photoUrl = document.getString("photoUrl") ?: ""

                    // Створюємо об'єкт користувача
                    val user = User(
                        id = document.id,
                        email = userEmail,
                        displayName = displayName,
                        role = role,
                        phone = phoneNumber,
                        photoUrl = photoUrl,
                        department = "",
                        badgeNumber = ""
                    )

                    // Відображаємо основні дані користувача
                    binding.textViewName.text = user.displayName
                    binding.textViewEmail.text = user.email

                    // Setup editable fields
                    binding.editTextName.setText(user.displayName)
                    binding.editTextPhone.setText(user.phone)

                    // Load profile image if available
                    if (user.photoUrl.isNotEmpty() && isAdded) {
                        Glide.with(requireContext())
                            .load(user.photoUrl)
                            .placeholder(R.drawable.ic_person_placeholder)
                            .error(R.drawable.ic_person_placeholder)
                            .circleCrop()
                            .into(binding.imageViewProfile)
                    }
                } else {
                    // Create a basic profile instead of showing error
                    createBasicProfile(userId, userEmail)
                }
            }
            .addOnFailureListener { e ->
                // Перевіряємо, чи фрагмент все ще прикріплений і binding не null
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Помилка завантаження даних: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createBasicProfile(userId: String, email: String) {
        // Create basic citizen profile
        val defaultName = email.substringBefore("@").capitalize()

        val newUser = User(
            id = userId,
            email = email,
            displayName = defaultName,
            role = "citizen",
            phone = "",
            photoUrl = "",
            department = "",
            badgeNumber = ""
        )

        // Save to Firestore
        db.collection("users").document(userId)
            .set(newUser)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Створено базовий профіль", Toast.LENGTH_SHORT).show()
                displayUserData(newUser)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Не вдалося створити профіль: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUserData(user: User) {
        binding.textViewName.text = user.displayName
        binding.textViewEmail.text = user.email

        // Setup editable fields
        binding.editTextName.setText(user.displayName)
        binding.editTextPhone.setText(user.phone)

        // Load profile image if available
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.imageViewProfile)
        }
    }

    private fun toggleEditMode(isEditing: Boolean) {
        // Toggle visibility of views
        binding.layoutViewMode.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.layoutEditMode.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.buttonEdit.visibility = if (isEditing) View.GONE else View.VISIBLE
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return
        val name = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.editTextName.error = "Ім'я не може бути порожнім"
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "displayName" to name
        )

        if (phone.isNotEmpty()) {
            updates["phoneNumber"] = phone
        }

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Профіль оновлено", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                loadUserData() // Reload data
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Помилка оновлення профілю: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "Заповніть всі поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Нові паролі не співпадають", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "Новий пароль має містити не менше 6 символів", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(currentPassword, newPassword, dialog)
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String, dialog: AlertDialog) {
        binding.progressBar.visibility = View.VISIBLE

        val user = auth.currentUser
        if (user == null || user.email == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Помилка аутентифікації", Toast.LENGTH_SHORT).show()
            return
        }

        // Перевірка поточного пароля
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Зміна пароля
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Пароль успішно змінено", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Помилка зміни пароля: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Поточний пароль невірний", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
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
        val dialog = AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "Введіть пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            deleteAccount(password, dialog)
        }
    }

    private fun deleteAccount(password: String, dialog: AlertDialog) {
        binding.progressBar.visibility = View.VISIBLE

        val user = auth.currentUser
        if (user == null || user.email == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Помилка аутентифікації", Toast.LENGTH_SHORT).show()
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
                                binding.progressBar.visibility = View.GONE
                                dialog.dismiss()
                                Toast.makeText(requireContext(), "Обліковий запис успішно видалено", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(requireContext(), LoginActivity::class.java))
                                requireActivity().finishAffinity()
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Помилка видалення облікового запису: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Помилка видалення даних користувача: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Пароль невірний", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}