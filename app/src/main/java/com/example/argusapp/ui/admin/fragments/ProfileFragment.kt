package com.example.argusapp.ui.admin.fragments

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
import com.example.argusapp.databinding.FragmentProfileBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        loadUserProfile()

        // Кнопка зміни фото
        binding.fabChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Кнопка виходу
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            binding.textViewEmail.text = user.email

            // Завантаження додаткових даних профілю з Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    // Перевірка, чи фрагмент все ще прикріплений до контексту
                    if (isAdded) {
                        if (document != null && document.exists()) {
                            binding.textViewName.text = document.getString("displayName") ?: "Адміністратор"
                            binding.textViewRole.text = "Адміністратор системи"

                            // Завантаження фото профілю
                            val photoUrl = document.getString("photoUrl")
                            if (!photoUrl.isNullOrEmpty()) {
                                Glide.with(requireContext())
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .error(R.drawable.ic_person_placeholder)
                                    .into(binding.imageViewProfile)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error loading profile data", e)
                    // Перевірка, чи фрагмент все ще прикріплений до контексту
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Помилка завантаження даних профілю", Toast.LENGTH_SHORT).show()
                    }
                }
        }
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
            Glide.with(requireContext())
                .load(selectedImageUri)
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
        val storageRef = storage.reference.child("profile_photos/admin/$userId/$fileName")

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
                Log.e("ProfileFragment", "Error uploading image", e)
                Toast.makeText(requireContext(), "Помилка завантаження зображення", Toast.LENGTH_SHORT).show()
                binding.fabChangePhoto.isEnabled = true
            }
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                Log.d("ProfileFragment", "Upload progress: $progress%")
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
                // Перевірка, чи фрагмент все ще прикріплений до контексту
                if (isAdded) {
                    Toast.makeText(requireContext(), "Фото профілю оновлено", Toast.LENGTH_SHORT).show()
                    binding.fabChangePhoto.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error updating photo URL", e)
                // Перевірка, чи фрагмент все ще прикріплений до контексту
                if (isAdded) {
                    Toast.makeText(requireContext(), "Помилка оновлення фото профілю", Toast.LENGTH_SHORT).show()
                    binding.fabChangePhoto.isEnabled = true
                }
            }
    }
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Вийти з облікового запису")
            .setMessage("Ви дійсно бажаєте вийти?")
            .setPositiveButton("Так") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Ні", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}