// ui/admin/fragments/SecurityAlertsFragment.kt
package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.data.model.SecurityAlert
import com.example.argusapp.databinding.FragmentSecurityAlertsBinding
import com.example.argusapp.ui.admin.adapters.SecurityAlertsAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SecurityAlertsFragment : Fragment() {

    private var _binding: FragmentSecurityAlertsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var currentFilter = "all" // "all", "new", "investigating", "resolved"

    companion object {
        private const val TAG = "SecurityAlertsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSecurityAlerts.layoutManager = LinearLayoutManager(requireContext())

        loadSecurityAlerts()

        binding.swipeRefresh.setOnRefreshListener {
            loadSecurityAlerts()
        }

        // Set up filter button
        binding.btnFilter.setOnClickListener {
            showFilterOptions()
        }

        // Set up severity filter chips
        binding.chipGroupSeverity.setOnCheckedStateChangeListener { _, _ ->
            loadSecurityAlerts()
        }
    }

    private fun loadSecurityAlerts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoAlerts.visibility = View.GONE

        // Build query based on filters
        var query: Query = db.collection("security_alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        // Apply status filter
        if (currentFilter != "all") {
            query = query.whereEqualTo("status", currentFilter)
        }

        // Get selected severity
        val selectedSeverity = when {
            binding.chipCritical.isChecked -> 5
            binding.chipHigh.isChecked -> 4
            binding.chipMedium.isChecked -> 3
            binding.chipLow.isChecked -> 2
            else -> 0 // All severities
        }

        // Apply severity filter
        if (selectedSeverity > 0) {
            query = query.whereEqualTo("severity", selectedSeverity)
        }

        query.get()
            .addOnSuccessListener { documents ->
                if (isAdded) {  // Check if fragment is still attached to activity
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    val alerts = documents.map { doc ->
                        val alert = doc.toObject(SecurityAlert::class.java)
                        alert.id = doc.id
                        alert
                    }

                    if (alerts.isEmpty()) {
                        binding.tvNoAlerts.visibility = View.VISIBLE
                        binding.rvSecurityAlerts.visibility = View.GONE
                    } else {
                        binding.tvNoAlerts.visibility = View.GONE
                        binding.rvSecurityAlerts.visibility = View.VISIBLE
                        binding.rvSecurityAlerts.adapter = SecurityAlertsAdapter(
                            alerts,
                            onAlertClick = { alert -> showAlertDetails(alert) }
                        )

                        // Update count badge
                        val newAlertsCount = alerts.count { it.status == "new" }
                        binding.tvNewAlertsCount.text = newAlertsCount.toString()
                        binding.tvNewAlertsCount.visibility = if (newAlertsCount > 0) View.VISIBLE else View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvNoAlerts.visibility = View.VISIBLE
                    binding.tvNoAlerts.text = "Помилка завантаження: ${e.message}"
                    Log.e(TAG, "Error getting security alerts", e)
                }
            }
    }

    private fun showFilterOptions() {
        // Create a dialog with filter options
        val options = arrayOf("Всі сповіщення", "Нові", "В розслідуванні", "Вирішені", "Помилкові")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Фільтр сповіщень")
            .setItems(options) { _, which ->
                currentFilter = when (which) {
                    0 -> "all"
                    1 -> "new"
                    2 -> "investigating"
                    3 -> "resolved"
                    4 -> "false_positive"
                    else -> "all"
                }
                loadSecurityAlerts()
            }
            .show()
    }

    private fun showAlertDetails(alert: SecurityAlert) {
        // Navigate to alert details fragment or show dialog
        val detailsDialog = SecurityAlertDetailsDialog(
            alert,
            onStatusChange = { updatedAlert ->
                updateAlertStatus(updatedAlert)
            }
        )
        detailsDialog.show(childFragmentManager, "AlertDetails")
    }

    private fun updateAlertStatus(alert: SecurityAlert) {
        db.collection("security_alerts").document(alert.id)
            .update(
                mapOf(
                    "status" to alert.status,
                    "notes" to alert.notes,
                    "resolvedBy" to alert.resolvedBy,
                    "resolvedAt" to alert.resolvedAt
                )
            )
            .addOnSuccessListener {
                loadSecurityAlerts()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating alert status", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}