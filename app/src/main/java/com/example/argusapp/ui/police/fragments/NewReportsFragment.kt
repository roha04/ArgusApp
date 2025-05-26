package com.example.argusapp.ui.police.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.argusapp.databinding.FragmentReportListBinding
import com.example.argusapp.data.model.Report
import com.example.argusapp.ui.police.PoliceReportDetailActivity
import com.example.argusapp.ui.police.adapters.PoliceReportsAdapter
import com.google.firebase.auth.FirebaseAuth

class NewReportsFragment : Fragment() {

    private var _binding: FragmentReportListBinding? = null
    private val binding get() = _binding!!

    private lateinit var reportsAdapter: PoliceReportsAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadReports()
    }

    private fun setupRecyclerView() {
        reportsAdapter = PoliceReportsAdapter { report ->
            // Обробка натискання на заявку
            val intent = Intent(requireContext(), PoliceReportDetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }

        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadReports() {
        showLoading(true)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("PoliceDebug", "Current user ID: $currentUserId")
        Log.d("PoliceDebug", "Looking for reports with assignedToId: $currentUserId")

        db.collection("reports")
            .whereEqualTo("status", "new")
            .whereEqualTo("assignedToId", currentUserId) // Add this line
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)

                val reports = documents.mapNotNull { doc ->
                    try {
                        val report = doc.toObject(Report::class.java)
                        report.id = doc.id
                        report
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(reports)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Помилка отримання даних: ${e.message}")
            }
    }

    private fun updateUI(reports: List<Report>) {
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