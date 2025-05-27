package com.example.argusapp.ui.citizen.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.FragmentCitizenProfileBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CitizenProfileFragment : Fragment() {

    private var _binding: FragmentCitizenProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserProfile()

        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.buttonEditProfile.setOnClickListener {
            // Navigate to edit profile screen or show edit dialog
            Toast.makeText(requireContext(), "Редагування профілю", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("Помилка аутентифікації")
            showLoading(false)
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.id = document.id
                    updateUI(user)
                } else {
                    showError("Профіль не знайдено")
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Помилка завантаження профілю: ${e.message}")
            }
    }

    private fun updateUI(user: User?) {
        if (user != null) {
            binding.textViewName.text = user.displayName
            binding.textViewEmail.text = user.email
            binding.textViewPhone.text = user.phone.ifEmpty { "Не вказано" }

            // Format and set registration date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val registrationDate = dateFormat.format(user.createdAt.toDate())
            binding.textViewRegistrationDate.text = registrationDate

            // Set user role with formatted display
            binding.textViewRole.text = when(user.role) {
                "citizen" -> "Громадянин"
                "police" -> "Поліцейський"
                "admin" -> "Адміністратор"
                else -> user.role.capitalize(Locale.getDefault())
            }

            // Set last active date
            val lastActiveDate = dateFormat.format(user.lastActive.toDate())
            binding.textViewLastActive.text = lastActiveDate
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

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.profileCard.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.buttonLogout.isEnabled = !isLoading
        binding.buttonEditProfile.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}