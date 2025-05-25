package com.example.argusapp.ui.police

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ActivityPoliceProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PoliceProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoliceProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoliceProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Профіль поліцейського"

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
    }

//    private fun loadUserData() {
//        val userId = auth.currentUser?.uid ?: return
//
//        binding.progressBar.visibility = View.VISIBLE
//
//        db.collection("users").document(userId)
//            .get()
//            .addOnSuccessListener { document ->
//                binding.progressBar.visibility = View.GONE
//
//                if (document != null && document.exists()) {
//                    val user = document.toObject(User::class.java)
//                    user?.id = document.id
//
//                    displayUserData(user)
//                } else {
//                    Toast.makeText(this, "Користувача не знайдено", Toast.LENGTH_SHORT).show()
//                    finish()
//                }
//            }
//            .addOnFailureListener { e ->
//                binding.progressBar.visibility = View.GONE
//                Toast.makeText(this, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
private fun loadUserData() {
    val userId = auth.currentUser?.uid ?: return
    val userEmail = auth.currentUser?.email ?: ""

    binding.progressBar.visibility = View.VISIBLE

    db.collection("users").document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                binding.progressBar.visibility = View.GONE
                val user = document.toObject(User::class.java)
                user?.id = document.id
                displayUserData(user)
            } else {
                // Create a basic profile instead of showing error
                createBasicProfile(userId, userEmail)
            }
        }
        .addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

    private fun createBasicProfile(userId: String, email: String) {
        // Create basic police profile
        val defaultName = email.substringBefore("@").capitalize()

        val newUser = User(
            id = userId,
            email = email,
            displayName = defaultName,
            role = "police",
            badgeNumber = "",
            department = "Районний відділ поліції",
            phone = "",
            photoUrl = ""
        )

        // Save to Firestore
        db.collection("users").document(userId)
            .set(newUser)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Створено базовий профіль", Toast.LENGTH_SHORT).show()
                displayUserData(newUser)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Не вдалося створити профіль: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUserData(user: User?) {
        user?.let {
            binding.textViewName.text = it.displayName
            binding.textViewEmail.text = it.email
            binding.textViewBadgeNumber.text = if (it.badgeNumber.isNotEmpty()) it.badgeNumber else "Не вказано"
            binding.textViewDepartment.text = if (it.department.isNotEmpty()) it.department else "Не вказано"

            // Setup editable fields
            binding.editTextName.setText(it.displayName)
            binding.editTextPhone.setText(it.phone)

            // Load profile image if available
            if (it.photoUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(it.photoUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(binding.imageViewProfile)
            }
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
            updates["phone"] = phone
        }

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Профіль оновлено", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                loadUserData() // Reload data
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Помилка оновлення профілю: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}