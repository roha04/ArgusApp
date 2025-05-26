package com.example.argusapp.ui.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.R
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
    private var reportsList: List<Report> = emptyList()

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
        setupFilterButton()
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

    private fun setupFilterButton() {
        binding.btnReportFilter.setOnClickListener { view ->
            showFilterPopupMenu(view)
        }
    }

    private fun showFilterPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menu.add(0, 0, 0, "Всі")
        popupMenu.menu.add(0, 1, 0, "Нові")
        popupMenu.menu.add(0, 2, 0, "В обробці")
        popupMenu.menu.add(0, 3, 0, "Вирішені")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> filterByStatus("all")
                1 -> filterByStatus("new")
                2 -> filterByStatus("in_progress")
                3 -> filterByStatus("resolved")
            }
            true
        }

        popupMenu.show()
    }

    private fun filterByStatus(status: String) {
        currentFilter = status

        // Update button text to show current filter
        val filterText = when (status) {
            "all" -> "Фільтр: Всі"
            "new" -> "Фільтр: Нові"
            "in_progress" -> "Фільтр: В обробці"
            "resolved" -> "Фільтр: Вирішені"
            else -> "Фільтр"
        }
        binding.btnReportFilter.text = filterText

        // If we already have reports loaded, we can filter them in memory
        if (reportsList.isNotEmpty() && currentFilter != "all") {
            val filteredReports = reportsList.filter { it.status == currentFilter }
            updateUI(filteredReports)
        } else if (reportsList.isNotEmpty() && currentFilter == "all") {
            // Show all reports
            updateUI(reportsList)
        } else {
            // Otherwise load from database
            loadReports()
        }
    }

    private fun loadReports() {
        // Check if fragment is still attached
        if (!isAdded) return

        showLoading(true)

        val query = when (currentFilter) {
            "all" -> db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING)
            else -> db.collection("reports").whereEqualTo("status", currentFilter)
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { documents ->
                // Check if binding is still valid
                if (_binding == null) return@addOnSuccessListener

                showLoading(false)

                reportsList = documents.mapNotNull { doc ->
                    try {
                        val report = doc.toObject(Report::class.java)
                        report.id = doc.id
                        report
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(reportsList)
            }
            .addOnFailureListener { e ->
                // Check if binding is still valid
                if (_binding == null) return@addOnFailureListener

                showLoading(false)
                showError("Помилка отримання даних: ${e.message}")
            }
    }

    private fun updateUI(reports: List<Report>) {
        // Check if binding is still valid
        if (_binding == null) return

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
        // Check if binding is still valid
        if (_binding == null) return

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        // Check if fragment is still attached
        if (!isAdded) return

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}