package com.example.argusapp.ui.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.argusapp.R
import com.example.argusapp.databinding.FragmentProfileBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        loadUserProfile()

        // Logout button
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            binding.textViewEmail.text = user.email

            // Load additional profile data from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.textViewName.text = document.getString("displayName") ?: "Адміністратор"
                        binding.textViewRole.text = "Адміністратор системи"
                    }
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