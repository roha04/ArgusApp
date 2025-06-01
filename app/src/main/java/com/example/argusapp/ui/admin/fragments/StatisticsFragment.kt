package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.argusapp.databinding.FragmentStatisticsBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    // Change this line to use safe call operator
    private val binding get() = _binding

    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        loadStatistics()
    }

    private fun loadStatistics() {
        showLoading(true)

        // Отримуємо початок сьогоднішнього дня
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time
        val startOfDayTimestamp = Timestamp(startOfDay)

        // Завантажуємо дані про користувачів
        loadUserStatistics()

        // Завантажуємо дані про заявки
        loadReportStatistics()

        // Завантажуємо дані про активність сьогодні
        loadActivityStatistics(startOfDayTimestamp)
    }

    private fun loadUserStatistics() {
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnSuccessListener

                var totalCount = 0
                var citizenCount = 0
                var policeCount = 0
                var adminCount = 0

                documents.forEach { doc ->
                    totalCount++

                    when (doc.getString("role")) {
                        "citizen" -> citizenCount++
                        "police" -> policeCount++
                        "admin" -> adminCount++
                    }
                }

                binding?.textViewTotalUsers?.text = totalCount.toString()
                binding?.textViewCitizenCount?.text = citizenCount.toString()
                binding?.textViewPoliceCount?.text = policeCount.toString()
                binding?.textViewAdminCount?.text = adminCount.toString()

                showLoading(false)
            }
            .addOnFailureListener { e ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnFailureListener

                showError("Помилка завантаження статистики користувачів: ${e.message}")
                showLoading(false)
            }
    }

    private fun loadReportStatistics() {
        db.collection("reports").get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnSuccessListener

                var totalCount = 0
                var newCount = 0
                var inProgressCount = 0
                var resolvedCount = 0

                documents.forEach { doc ->
                    totalCount++

                    when (doc.getString("status")) {
                        "new" -> newCount++
                        "in_progress" -> inProgressCount++
                        "resolved" -> resolvedCount++
                    }
                }

                binding?.textViewTotalReports?.text = totalCount.toString()
                binding?.textViewNewReports?.text = newCount.toString()
                binding?.textViewInProgressReports?.text = inProgressCount.toString()
                binding?.textViewResolvedReports?.text = resolvedCount.toString()
            }
            .addOnFailureListener { e ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnFailureListener

                showError("Помилка завантаження статистики заявок: ${e.message}")
            }
    }

    private fun loadActivityStatistics(startOfDay: Timestamp) {
        // Заявки за сьогодні
        db.collection("reports")
            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
            .get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnSuccessListener

                binding?.textViewTodayReports?.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnFailureListener

                showError("Помилка завантаження статистики активності: ${e.message}")
            }

        // Реєстрації за сьогодні
        db.collection("users")
            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
            .get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnSuccessListener

                binding?.textViewTodayRegistrations?.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@addOnFailureListener

                showError("Помилка завантаження статистики активності: ${e.message}")
            }
    }

    private fun showLoading(isLoading: Boolean) {
        // Use safe call operator
        binding?.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
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