// ui/admin/fragments/SecurityAlertDetailsDialog.kt
package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.argusapp.R
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.data.model.SecurityAlert
import com.example.argusapp.databinding.DialogSecurityAlertDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SecurityAlertDetailsDialog(
    private val alert: SecurityAlert,
    private val onStatusChange: (SecurityAlert) -> Unit
) : DialogFragment() {

    private var _binding: DialogSecurityAlertDetailsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSecurityAlertDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog title
        binding.tvDialogTitle.text = "Деталі сповіщення"

        // Populate alert details
        populateAlertDetails()

        // Load related logs
        loadRelatedLogs()

        // Set up status buttons
        setupStatusButtons()

        // Set up close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Set up save button
        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun populateAlertDetails() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        binding.tvAlertType.text = alert.getAlertTypeText()
        binding.tvDescription.text = alert.description
        binding.tvDetails.text = alert.details
        binding.tvUserEmail.text = alert.userEmail
        binding.tvUserType.text = alert.userType
        binding.tvTimestamp.text = dateFormat.format(alert.timestamp.toDate())
        binding.tvSeverity.text = alert.getSeverityText()

        // Set current status
        when (alert.status) {
            "new" -> binding.rbStatusNew.isChecked = true
            "investigating" -> binding.rbStatusInvestigating.isChecked = true
            "resolved" -> binding.rbStatusResolved.isChecked = true
            "false_positive" -> binding.rbStatusFalsePositive.isChecked = true
        }

        // Set notes
        binding.etNotes.setText(alert.notes)

        // Show resolution info if resolved
        if (alert.status == "resolved" || alert.status == "false_positive") {
            binding.layoutResolution.visibility = View.VISIBLE
            if (alert.resolvedAt != null && alert.resolvedBy.isNotEmpty()) {
                binding.tvResolvedBy.text = alert.resolvedBy
                binding.tvResolvedAt.text = dateFormat.format(alert.resolvedAt!!.toDate())
            }
        } else {
            binding.layoutResolution.visibility = View.GONE
        }
    }

    private fun loadRelatedLogs() {
        binding.progressBarLogs.visibility = View.VISIBLE
        binding.tvNoRelatedLogs.visibility = View.GONE
        binding.layoutRelatedLogs.visibility = View.GONE

        if (alert.relatedLogIds.isEmpty()) {
            binding.progressBarLogs.visibility = View.GONE
            binding.tvNoRelatedLogs.visibility = View.VISIBLE
            return
        }

        // Get the first 5 related logs
        val logIds = alert.relatedLogIds.take(5)
        val logs = mutableListOf<ActivityLog>()

        // Use a counter to track when all queries are complete
        var completedQueries = 0

        for (logId in logIds) {
            db.collection("activity_logs").document(logId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val log = document.toObject(ActivityLog::class.java)
                        log?.id = document.id
                        log?.let { logs.add(it) }
                    }

                    completedQueries++

                    // Check if all queries are complete
                    if (completedQueries == logIds.size) {
                        displayRelatedLogs(logs)
                    }
                }
                .addOnFailureListener {
                    completedQueries++

                    // Check if all queries are complete
                    if (completedQueries == logIds.size) {
                        displayRelatedLogs(logs)
                    }
                }
        }
    }

    private fun displayRelatedLogs(logs: List<ActivityLog>) {
        if (!isAdded) return

        binding.progressBarLogs.visibility = View.GONE

        if (logs.isEmpty()) {
            binding.tvNoRelatedLogs.visibility = View.VISIBLE
            binding.layoutRelatedLogs.visibility = View.GONE
            return
        }

        binding.tvNoRelatedLogs.visibility = View.GONE
        binding.layoutRelatedLogs.visibility = View.VISIBLE

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        // Display up to 3 logs
        val logsToShow = logs.sortedByDescending { it.timestamp }.take(3)

        // Log 1
        if (logsToShow.size > 0) {
            binding.layoutLog1.visibility = View.VISIBLE
            binding.tvLogAction1.text = logsToShow[0].action
            binding.tvLogTime1.text = dateFormat.format(logsToShow[0].timestamp.toDate())
            binding.tvLogDetails1.text = logsToShow[0].details
        } else {
            binding.layoutLog1.visibility = View.GONE
        }

        // Log 2
        if (logsToShow.size > 1) {
            binding.layoutLog2.visibility = View.VISIBLE
            binding.tvLogAction2.text = logsToShow[1].action
            binding.tvLogTime2.text = dateFormat.format(logsToShow[1].timestamp.toDate())
            binding.tvLogDetails2.text = logsToShow[1].details
        } else {
            binding.layoutLog2.visibility = View.GONE
        }

        // Log 3
        if (logsToShow.size > 2) {
            binding.layoutLog3.visibility = View.VISIBLE
            binding.tvLogAction3.text = logsToShow[2].action
            binding.tvLogTime3.text = dateFormat.format(logsToShow[2].timestamp.toDate())
            binding.tvLogDetails3.text = logsToShow[2].details
        } else {
            binding.layoutLog3.visibility = View.GONE
        }

        // Show "View All" button if there are more logs
        if (alert.relatedLogIds.size > 3) {
            binding.btnViewAllLogs.visibility = View.VISIBLE
            binding.btnViewAllLogs.text = "Переглянути всі (${alert.relatedLogIds.size})"
            binding.btnViewAllLogs.setOnClickListener {
                // Navigate to a full list of related logs
                // This would be implemented in a separate fragment
                showAllRelatedLogs()
            }
        } else {
            binding.btnViewAllLogs.visibility = View.GONE
        }
    }

    private fun showAllRelatedLogs() {
        // This would navigate to a fragment showing all related logs
        // For now, we'll just show a toast
        android.widget.Toast.makeText(
            requireContext(),
            "Ця функція буде реалізована в наступній версії",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupStatusButtons() {
        binding.radioGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            // Show resolution layout if status is resolved or false positive
            binding.layoutResolution.visibility = if (
                checkedId == R.id.rbStatusResolved ||
                checkedId == R.id.rbStatusFalsePositive
            ) View.VISIBLE else View.GONE
        }
    }

    private fun saveChanges() {
        // Get current user
        val currentUser = auth.currentUser ?: return

        // Get selected status
        val newStatus = when (binding.radioGroupStatus.checkedRadioButtonId) {
            R.id.rbStatusNew -> "new"
            R.id.rbStatusInvestigating -> "investigating"
            R.id.rbStatusResolved -> "resolved"
            R.id.rbStatusFalsePositive -> "false_positive"
            else -> alert.status
        }

        // Update alert object
        alert.status = newStatus
        alert.notes = binding.etNotes.text.toString()

        // If status changed to resolved or false positive, update resolution info
        if ((newStatus == "resolved" || newStatus == "false_positive") &&
            (alert.resolvedAt == null || alert.resolvedBy.isEmpty())) {
            alert.resolvedAt = Timestamp.now()

            // Get current user's name from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val displayName = document.getString("displayName") ?: currentUser.email ?: "Unknown"
                        alert.resolvedBy = displayName

                        // Save changes
                        onStatusChange(alert)
                        dismiss()
                    }
                }
                .addOnFailureListener {
                    // Use email as fallback
                    alert.resolvedBy = currentUser.email ?: "Unknown"

                    // Save changes
                    onStatusChange(alert)
                    dismiss()
                }
        } else {
            // Save changes
            onStatusChange(alert)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}