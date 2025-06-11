package com.example.argusapp.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.data.model.Department
import com.example.argusapp.databinding.ActivityRegisterDepartmentBinding
import com.example.argusapp.ui.common.MapSelectionActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class RegisterDepartmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterDepartmentBinding
    private val db = FirebaseFirestore.getInstance()
    private var selectedLocation: GeoPoint? = null

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDepartmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupListeners()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Register Department"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupListeners() {
        binding.btnSelectLocation.setOnClickListener {
            val intent = Intent(this, MapSelectionActivity::class.java)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
        }

        binding.btnRegisterDepartment.setOnClickListener {
            registerDepartment()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

            selectedLocation = GeoPoint(latitude, longitude)
            binding.tvSelectedLocation.text = "Selected: Lat: $latitude, Lng: $longitude"
        }
    }

    private fun registerDepartment() {
        // Validate inputs
        val name = binding.etDepartmentName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val jurisdictionRadiusStr = binding.etJurisdictionRadius.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilDepartmentName.error = "Department name is required"
            return
        }

        if (address.isEmpty()) {
            binding.tilAddress.error = "Address is required"
            return
        }

        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location for the department", Toast.LENGTH_SHORT).show()
            return
        }

        val jurisdictionRadius = if (jurisdictionRadiusStr.isNotEmpty()) {
            jurisdictionRadiusStr.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        // Show progress
        binding.progressBar.visibility = View.VISIBLE

        // Create department object
        val department = Department(
            name = name,
            address = address,
            phoneNumber = phoneNumber,
            email = email,
            description = description,
            location = selectedLocation,
            jurisdictionRadius = jurisdictionRadius,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        // Save to Firestore
        db.collection("departments")
            .add(department)
            .addOnSuccessListener { documentReference ->
                // Update the department with its ID
                val departmentId = documentReference.id
                documentReference.update("id", departmentId)
                    .addOnSuccessListener {
                        // Log the activity
                        logActivity("Created new department: $name")

                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Department registered successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error updating department ID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error registering department: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logActivity(description: String) {
        val activityLog = ActivityLog(
            action = "DEPARTMENT_CREATED",
            //description = description,
            timestamp = Timestamp.now(),
            userId = getCurrentUserId()
        )

        db.collection("activityLogs")
            .add(activityLog)
            .addOnFailureListener { e ->
                // Just log the error, don't block the UI
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