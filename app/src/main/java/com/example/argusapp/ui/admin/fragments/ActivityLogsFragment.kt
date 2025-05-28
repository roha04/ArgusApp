// ui/admin/fragments/ActivityLogsFragment.kt
package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.databinding.FragmentActivityLogsBinding
import com.example.argusapp.ui.admin.adapters.ActivityLogsAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ActivityLogsFragment : Fragment() {

    private var _binding: FragmentActivityLogsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ActivityLogsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvActivityLogs.layoutManager = LinearLayoutManager(requireContext())

        loadActivityLogs()

        binding.swipeRefresh.setOnRefreshListener {
            loadActivityLogs()
        }

        // Set up filter button
        binding.btnFilter.setOnClickListener {
            showFilterOptions()
        }
    }

    private fun loadActivityLogs() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoLogs.visibility = View.GONE

        db.collection("activity_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {  // Check if fragment is still attached to activity
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    val logs = documents.map { doc ->
                        val log = doc.toObject(ActivityLog::class.java)
                        log.id = doc.id
                        log
                    }

                    if (logs.isEmpty()) {
                        binding.tvNoLogs.visibility = View.VISIBLE
                        binding.rvActivityLogs.visibility = View.GONE
                    } else {
                        binding.tvNoLogs.visibility = View.GONE
                        binding.rvActivityLogs.visibility = View.VISIBLE
                        binding.rvActivityLogs.adapter = ActivityLogsAdapter(logs)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvNoLogs.visibility = View.VISIBLE
                    binding.tvNoLogs.text = "Помилка завантаження: ${e.message}"
                    Log.e(TAG, "Error getting activity logs", e)
                }
            }
    }

    private fun filterByAction(action: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoLogs.visibility = View.GONE

        db.collection("activity_logs")
            .whereEqualTo("action", action)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE

                    val logs = documents.map { doc ->
                        val log = doc.toObject(ActivityLog::class.java)
                        log.id = doc.id
                        log
                    }

                    if (logs.isEmpty()) {
                        binding.tvNoLogs.visibility = View.VISIBLE
                        binding.rvActivityLogs.visibility = View.GONE
                    } else {
                        binding.tvNoLogs.visibility = View.GONE
                        binding.rvActivityLogs.visibility = View.VISIBLE
                        binding.rvActivityLogs.adapter = ActivityLogsAdapter(logs)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoLogs.visibility = View.VISIBLE
                    binding.tvNoLogs.text = "Помилка завантаження: ${e.message}"
                    Log.e(TAG, "Error filtering activity logs", e)
                }
            }
    }

    private fun showFilterOptions() {
        // Create a dialog with filter options
        val options = arrayOf("Всі записи", "Входи в систему", "Реєстрації", "Зміни статусу заявок")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Фільтр журналу")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> loadActivityLogs() // All logs
                    1 -> filterByAction("Вхід в систему")
                    2 -> filterByAction("Реєстрація користувача")
                    3 -> filterByAction("Змінено статус заявки")
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}