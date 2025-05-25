package com.example.argusapp.ui.admin.fragments


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.databinding.FragmentReportsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.argusapp.data.model.Report
import com.example.argusapp.ui.admin.AdminReportDetailActivity
import com.example.argusapp.ui.admin.adapters.AdminReportsAdapter

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var reportsAdapter: AdminReportsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentFilter: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupChipGroupListeners()
        loadReports()
    }

    private fun setupRecyclerView() {
        reportsAdapter = AdminReportsAdapter { report ->
            // Обробка натискання на заявку
            val intent = Intent(requireContext(), AdminReportDetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }

        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupChipGroupListeners() {
        binding.chipAll.setOnClickListener {
            currentFilter = "all"
            loadReports()
        }
        binding.chipNew.setOnClickListener {
            currentFilter = "new"
            loadReports()
        }
        binding.chipInProgress.setOnClickListener {
            currentFilter = "in_progress"
            loadReports()
        }
        binding.chipResolved.setOnClickListener {
            currentFilter = "resolved"
            loadReports()
        }
    }

    private fun loadReports() {
        showLoading(true)

        val query = when (currentFilter) {
            "all" -> db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING)
            else -> db.collection("reports").whereEqualTo("status", currentFilter)
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.get()
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