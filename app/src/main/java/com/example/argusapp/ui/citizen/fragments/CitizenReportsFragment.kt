package com.example.argusapp.ui.citizen.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.data.model.Report
import com.example.argusapp.databinding.FragmentCitizenReportsBinding
import com.example.argusapp.ui.citizen.CreateReportActivity
import com.example.argusapp.ui.citizen.ReportDetailsActivity
import com.example.argusapp.ui.citizen.adapters.ReportsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CitizenReportsFragment : Fragment() {

    private var _binding: FragmentCitizenReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var reportsAdapter: ReportsAdapter

    // Add a variable to store the listener registration
    private var reportsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitizenReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadReports()
    }

    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter { report ->
            // Handle report click
            val intent = Intent(requireContext(), ReportDetailsActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }

        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadReports() {
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showError("Помилка аутентифікації")
            return
        }

        // Remove any existing listener
        reportsListener?.remove()

        // Set up the new listener and store the registration
        reportsListener = db.collection("reports")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                // Check if the fragment is still attached to avoid NPE
                if (!isAdded || _binding == null) return@addSnapshotListener

                showLoading(false)

                if (e != null) {
                    showError("Помилка отримання даних: ${e.message}")
                    return@addSnapshotListener
                }

                val reports = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val report = doc.toObject(Report::class.java)
                        report?.id = doc.id
                        report
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                updateUI(reports)
            }
    }

    private fun updateUI(reports: List<Report>) {
        // Check if the fragment is still attached to avoid NPE
        if (!isAdded || _binding == null) return

        if (reports.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewReports.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewReports.visibility = View.VISIBLE
            reportsAdapter.submitList(reports)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // Check if the fragment is still attached to avoid NPE
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        // Check if the fragment is still attached to avoid NPE
        if (!isAdded) return

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    // Method to handle FAB click (will be called from activity)
    fun onAddReportClicked() {
        startActivity(Intent(requireContext(), CreateReportActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the listener when the view is destroyed
        reportsListener?.remove()
        reportsListener = null
        _binding = null
    }
}