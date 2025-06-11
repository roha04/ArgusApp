package com.example.argusapp.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.data.model.Department
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ActivityAssignOfficersBinding
import com.example.argusapp.ui.admin.adapters.OfficerAssignmentAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AssignOfficersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssignOfficersBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var officerAdapter: OfficerAssignmentAdapter
    private val departments = mutableListOf<Department>()
    private val officers = mutableListOf<User>()
    private var selectedDepartmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignOfficersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupRecyclerView()
        loadDepartments()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Assign Officers to Department"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        officerAdapter = OfficerAssignmentAdapter(officers) { user, isAssigned ->
            if (selectedDepartmentId != null) {
                updateOfficerDepartment(user, isAssigned)
            } else {
                Toast.makeText(this, "Please select a department first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvOfficers.apply {
            layoutManager = LinearLayoutManager(this@AssignOfficersActivity)
            adapter = officerAdapter
        }
    }

    private fun loadDepartments() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("departments")
            .get()
            .addOnSuccessListener { result ->
                departments.clear()
                for (document in result) {
                    val department = document.toObject(Department::class.java).apply {
                        id = document.id
                    }
                    departments.add(department)
                }

                setupDepartmentSpinner()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading departments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDepartmentSpinner() {
        val departmentNames = departments.map { it.name }.toMutableList()
        departmentNames.add(0, "Вибрати відділ")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, departmentNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerDepartments.adapter = adapter
        binding.spinnerDepartments.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedDepartmentId = departments[position - 1].id
                    loadOfficers()
                } else {
                    selectedDepartmentId = null
                    officers.clear()
                    officerAdapter.notifyDataSetChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDepartmentId = null
            }
        }
    }

private fun loadOfficers() {
    binding.progressBar.visibility = View.VISIBLE
    binding.tvNoOfficers.visibility = View.GONE
    binding.rvOfficers.visibility = View.GONE

    db.collection("users")
        .whereEqualTo("role", "police")
        .get()
        .addOnSuccessListener { result ->
            officers.clear()
            for (document in result) {
                val officer = document.toObject(User::class.java).apply {
                    id = document.id
                }
                officers.add(officer)
            }

            // Mark officers who are already assigned to this department
            officers.forEach { officer ->
                officer.isSelected = officer.department == selectedDepartmentId
            }

            officerAdapter.notifyDataSetChanged()
            binding.progressBar.visibility = View.GONE

            // Show "No officers" message if the list is empty
            if (officers.isEmpty()) {
                binding.tvNoOfficers.visibility = View.VISIBLE
                binding.rvOfficers.visibility = View.GONE
            } else {
                binding.tvNoOfficers.visibility = View.GONE
                binding.rvOfficers.visibility = View.VISIBLE
            }
        }
        .addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            binding.tvNoOfficers.visibility = View.VISIBLE
            binding.rvOfficers.visibility = View.GONE
            Toast.makeText(this, "Error loading officers: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
    private fun updateOfficerDepartment(officer: User, isAssigned: Boolean) {
        val departmentId = if (isAssigned) selectedDepartmentId else ""
        val departmentName = if (isAssigned) {
            departments.find { it.id == selectedDepartmentId }?.name ?: ""
        } else {
            ""
        }

        db.collection("users").document(officer.id)
            .update("department", departmentId ?: "")
            .addOnSuccessListener {
                val action = if (isAssigned) "assigned to" else "removed from"
                val message = "Officer ${officer.displayName} $action department $departmentName"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // Log the activity
                logActivity(message)

                // Update department officer count
                updateDepartmentOfficerCount(departmentId ?: "")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating officer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDepartmentOfficerCount(departmentId: String) {
        if (departmentId.isEmpty()) return

        db.collection("users")
            .whereEqualTo("department", departmentId)
            .whereEqualTo("role", "police")
            .get()
            .addOnSuccessListener { result ->
                val officerCount = result.size()

                db.collection("departments").document(departmentId)
                    .update("officerCount", officerCount)
                    .addOnFailureListener { e ->
                        println("Error updating department officer count: ${e.message}")
                    }
            }
    }

    private fun logActivity(description: String) {
        val activityLog = ActivityLog(
            action = "OFFICER_ASSIGNMENT",
            //description = description,
            timestamp = Timestamp.now(),
            userId = getCurrentUserId()
        )

        db.collection("activityLogs")
            .add(activityLog)
            .addOnFailureListener { e ->
                println("Error logging activity: ${e.message}")
            }
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}