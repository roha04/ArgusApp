package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.example.argusapp.databinding.FragmentUsersBinding
import com.example.argusapp.data.model.User
import com.example.argusapp.ui.admin.adapters.UsersAdapter

class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var usersAdapter: UsersAdapter
    private lateinit var db: FirebaseFirestore
    private var currentFilter: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupChipGroupListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter { user ->
            // Обробка натискання на користувача, наприклад, перегляд деталей
            Toast.makeText(context, "Вибрано: ${user.displayName}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = usersAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupChipGroupListeners() {
        binding.chipAll.setOnClickListener {
            currentFilter = "all"
            loadUsers()
        }
        binding.chipAdmins.setOnClickListener {
            currentFilter = "admin"
            loadUsers()
        }
        binding.chipPolice.setOnClickListener {
            currentFilter = "police"
            loadUsers()
        }
        binding.chipCitizens.setOnClickListener {
            currentFilter = "citizen"
            loadUsers()
        }
    }

    private fun loadUsers() {
        showLoading(true)

        val query = when (currentFilter) {
            "all" -> db.collection("users")
            else -> db.collection("users").whereEqualTo("role", currentFilter)
        }

        query.get()
            .addOnSuccessListener { documents ->
                showLoading(false)

                val users = documents.mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        user.id = doc.id
                        user
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(users)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Помилка отримання даних: ${e.message}")
            }
    }

    private fun updateUI(users: List<User>) {
        if (users.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewUsers.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewUsers.visibility = View.VISIBLE
            usersAdapter.submitList(users)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}