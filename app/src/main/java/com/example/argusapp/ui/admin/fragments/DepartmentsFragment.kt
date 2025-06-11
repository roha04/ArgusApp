package com.example.argusapp.ui.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.databinding.FragmentDepartmentsBinding
import com.example.argusapp.data.model.Department
import com.example.argusapp.ui.admin.DepartmentMapActivity
import com.example.argusapp.ui.admin.RegisterDepartmentActivity
import com.example.argusapp.ui.admin.AssignOfficersActivity
import com.example.argusapp.ui.admin.DepartmentDetailActivity
import com.example.argusapp.ui.admin.adapters.DepartmentsAdapter
import com.google.firebase.firestore.FirebaseFirestore

class DepartmentsFragment : Fragment() {

    private var _binding: FragmentDepartmentsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var departmentsAdapter: DepartmentsAdapter
    private val departments = mutableListOf<Department>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadDepartments()
    }

    private fun setupRecyclerView() {
        departmentsAdapter = DepartmentsAdapter(departments) { department ->
            // Navigate to department details when a department is clicked
            val intent = Intent(requireContext(), DepartmentDetailActivity::class.java)
            intent.putExtra("departmentId", department.id)
            startActivity(intent)
        }

        binding.rvDepartments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = departmentsAdapter
        }
    }

    private fun setupButtons() {
        binding.fabAddDepartment.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterDepartmentActivity::class.java))
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(requireContext(), DepartmentMapActivity::class.java))
        }

        binding.btnAssignOfficers.setOnClickListener {
            startActivity(Intent(requireContext(), AssignOfficersActivity::class.java))
        }
    }

    private fun loadDepartments() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoDepartments.visibility = View.GONE

        db.collection("departments")
            .get()
            .addOnSuccessListener { result ->
                val newDepartments = result.documents.mapNotNull { document ->
                    try {
                        document.toObject(Department::class.java)?.apply {
                            id = document.id

                            // Явно встановлюємо isActive, якщо поле є в документі
                            if (document.contains("isActive")) {
                                isActive = document.getBoolean("isActive") ?: true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DepartmentsFragment", "Error converting document: ${e.message}")
                        null
                    }
                }

                departmentsAdapter.updateDepartments(newDepartments)

                binding.tvNoDepartments.visibility = if (newDepartments.isEmpty()) View.VISIBLE else View.GONE
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoDepartments.visibility = View.VISIBLE
                Toast.makeText(context, "Помилка завантаження відділів: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        loadDepartments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}