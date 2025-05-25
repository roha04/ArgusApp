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
    private var usersList: List<User> = emptyList()

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

    private fun loadUsers() {
        showLoading(true)

        val query = when (currentFilter) {
            "all" -> db.collection("users")
            else -> db.collection("users").whereEqualTo("role", currentFilter)
        }

        query.get()
            .addOnSuccessListener { documents ->
                showLoading(false)

                usersList = documents.mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        user.id = doc.id
                        user
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(usersList)
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

    /**
     * Method to filter users by role from the activity
     * This will be called from the AdminMainActivity when the filter button is clicked
     */
    fun filterByRole(role: String?) {
        currentFilter = when (role) {
            null, "all" -> "all"
            "admin" -> "admin"
            "police" -> "police"
            "citizen", "user" -> "citizen"
            else -> "all"
        }

        // If we already have users loaded, we can filter them in memory
        if (usersList.isNotEmpty() && currentFilter != "all") {
            val filteredUsers = usersList.filter { it.role == currentFilter }
            updateUI(filteredUsers)
        } else if (usersList.isNotEmpty() && currentFilter == "all") {
            // Show all users
            updateUI(usersList)
        } else {
            // Otherwise load from database
            loadUsers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
