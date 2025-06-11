package com.example.argusapp.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.R
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.data.model.Department
import com.example.argusapp.databinding.ActivityDepartmentDetailBinding
import com.example.argusapp.ui.common.MapSelectionActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class DepartmentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepartmentDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var departmentId: String? = null
    private var department: Department? = null
    private var selectedLocation: GeoPoint? = null
    private var isEditMode = false

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepartmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        departmentId = intent.getStringExtra("departmentId")
        if (departmentId == null) {
            Toast.makeText(this, "Department ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupActionBar()
        loadDepartmentDetails()
        setupListeners()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Деталі відділу"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun loadDepartmentDetails() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("departments").document(departmentId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Перевіряємо, чи є поле isActive в документі
                    val isActiveExists = document.contains("isActive")
                    val isActiveValue = document.getBoolean("isActive") ?: true

                    Log.d("DepartmentDetail", "Document ${document.id}, isActiveExists: $isActiveExists, isActiveValue: $isActiveValue")

                    department = document.toObject(Department::class.java)?.apply {
                        id = document.id
                    }

                    Log.d("DepartmentDetail", "After conversion: ${department?.name}, isActive: ${department?.isActive}")

                    // Якщо значення isActive не встановлено правильно, встановлюємо його явно
                    if (department != null && isActiveExists && department?.isActive != isActiveValue) {
                        Log.d("DepartmentDetail", "Fixing isActive value")
                        department?.isActive = isActiveValue
                    }

                    if (department != null) {
                        displayDepartmentDetails()
                        selectedLocation = department?.location
                    } else {
                        Toast.makeText(this, "Failed to load department details", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Department not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading department: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayDepartmentDetails() {
        department?.let { dept ->
            binding.tvDepartmentName.text = dept.name
            binding.tvDepartmentAddress.text = dept.address
            binding.tvDepartmentPhone.text = dept.phoneNumber.ifEmpty { "No phone number" }
            binding.tvDepartmentEmail.text = dept.email.ifEmpty { "No email" }
            binding.tvDepartmentDescription.text = dept.description.ifEmpty { "No description" }

            val locationText = if (dept.location != null) {
                "Lat: ${dept.location!!.latitude}, Lng: ${dept.location!!.longitude}"
            } else {
                "No location set"
            }
            binding.tvDepartmentLocation.text = locationText

            binding.tvJurisdictionRadius.text = "${dept.jurisdictionRadius} km"


            // Set status indicator
            binding.switchStatus.isChecked = dept.isActive

            // Set edit fields (initially invisible)
            binding.etDepartmentName.setText(dept.name)
            binding.etDepartmentAddress.setText(dept.address)
            binding.etDepartmentPhone.setText(dept.phoneNumber)
            binding.etDepartmentEmail.setText(dept.email)
            binding.etDepartmentDescription.setText(dept.description)
            binding.etJurisdictionRadius.setText(dept.jurisdictionRadius.toString())
        }
    }

    private fun setupListeners() {
        binding.btnViewOnMap.setOnClickListener {
            if (department?.location != null) {
                val intent = Intent(this, DepartmentMapActivity::class.java)
                intent.putExtra("focusDepartmentId", departmentId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No location set for this department", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAssignOfficers.setOnClickListener {
            val intent = Intent(this, AssignOfficersActivity::class.java)
            intent.putExtra("departmentId", departmentId)
            startActivity(intent)
        }

        binding.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (department?.isActive != isChecked) {
                updateDepartmentStatus(isChecked)
            }
        }

        binding.btnSelectLocation.setOnClickListener {
            if (isEditMode) {
                val intent = Intent(this, MapSelectionActivity::class.java)
                startActivityForResult(intent, LOCATION_REQUEST_CODE)
            }
        }

        binding.btnSaveChanges.setOnClickListener {
            if (isEditMode) {
                saveDepartmentChanges()
            }
        }
        binding.btnEditDepartment.setOnClickListener {
            toggleEditMode()
        }
    }

    private fun updateDepartmentStatus(isActive: Boolean) {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("departments").document(departmentId!!)
            .update("isActive", isActive)
            .addOnSuccessListener {
                department?.isActive = isActive

                val statusText = if (isActive) "активовано" else "деактивовано"
                Toast.makeText(this, "Відділ $statusText", Toast.LENGTH_SHORT).show()

                // Log activity
                logActivity("Department ${department?.name} $statusText")

                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Помилка оновлення статусу: ${e.message}", Toast.LENGTH_SHORT).show()
                println("Error updating status: ${e}")

                // Reset switch to original value
                binding.switchStatus.setOnCheckedChangeListener { _, isChecked ->
                    println("Switch changed to: $isChecked, current department status: ${department?.isActive}")
                    if (department?.isActive != isChecked) {
                        updateDepartmentStatus(isChecked)
                    }
                }
            }
    }

    private fun saveDepartmentChanges() {
        // Validate inputs
        val name = binding.etDepartmentName.text.toString().trim()
        val address = binding.etDepartmentAddress.text.toString().trim()
        val phoneNumber = binding.etDepartmentPhone.text.toString().trim()
        val email = binding.etDepartmentEmail.text.toString().trim()
        val description = binding.etDepartmentDescription.text.toString().trim()
        val jurisdictionRadiusStr = binding.etJurisdictionRadius.text.toString().trim()

        if (name.isEmpty()) {
            binding.etDepartmentName.error = "Department name is required"
            return
        }

        if (address.isEmpty()) {
            binding.etDepartmentAddress.error = "Address is required"
            return
        }

        val jurisdictionRadius = if (jurisdictionRadiusStr.isNotEmpty()) {
            jurisdictionRadiusStr.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        binding.progressBar.visibility = View.VISIBLE

        // Create update map
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "address" to address,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "description" to description,
            "jurisdictionRadius" to jurisdictionRadius,
            "updatedAt" to Timestamp.now()
        )

        // Add location if selected
        if (selectedLocation != null) {
            updates["location"] = selectedLocation!!
        }

        // Update in Firestore
        db.collection("departments").document(departmentId!!)
            .update(updates)
            .addOnSuccessListener {
                // Update local department object
                department?.apply {
                    this.name = name
                    this.address = address
                    this.phoneNumber = phoneNumber
                    this.email = email
                    this.description = description
                    this.jurisdictionRadius = jurisdictionRadius
                    this.location = selectedLocation ?: this.location
                    this.updatedAt = Timestamp.now()
                }

                // Log activity
                logActivity("Updated department: $name")

                // Switch back to view mode
                toggleEditMode()
                displayDepartmentDetails()

                Toast.makeText(this, "Department updated successfully", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error updating department: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        // Toggle visibility of view and edit layouts
        binding.viewLayout.visibility = if (isEditMode) View.GONE else View.VISIBLE
        binding.editLayout.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // Update action bar
        supportActionBar?.title = if (isEditMode) "Edit Department" else "Department Details"
        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

            selectedLocation = GeoPoint(latitude, longitude)
            binding.tvSelectedLocationEdit.text = "Selected: Lat: $latitude, Lng: $longitude"
        }
    }

    private fun confirmDeleteDepartment() {
        AlertDialog.Builder(this)
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete this department? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDepartment()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDepartment() {
        binding.progressBar.visibility = View.VISIBLE

        // First, update any officers assigned to this department
        db.collection("users")
            .whereEqualTo("department", departmentId)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()

                // Update each officer to remove department assignment
                for (document in result) {
                    val officerRef = db.collection("users").document(document.id)
                    batch.update(officerRef, "department", "")
                }

                // Now delete the department
                batch.delete(db.collection("departments").document(departmentId!!))

                // Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        // Log activity
                        logActivity("Deleted department: ${department?.name}")

                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Department deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error deleting department: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error updating officers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logActivity(description: String) {
        val activityLog = ActivityLog(
            action = "DEPARTMENT_MANAGEMENT",
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_department_detail, menu)

        // Show/hide menu items based on edit mode
        menu?.findItem(R.id.action_edit)?.isVisible = !isEditMode
        menu?.findItem(R.id.action_delete)?.isVisible = !isEditMode
        menu?.findItem(R.id.action_cancel)?.isVisible = isEditMode

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                toggleEditMode()
                true
            }
            R.id.action_delete -> {
                confirmDeleteDepartment()
                true
            }
            R.id.action_cancel -> {
                toggleEditMode()
                true
            }
            android.R.id.home -> {
                if (isEditMode) {
                    toggleEditMode()
                    true
                } else {
                    onBackPressed()
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (isEditMode) {
            toggleEditMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}