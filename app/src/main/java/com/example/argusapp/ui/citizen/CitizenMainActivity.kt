package com.example.argusapp.ui.citizen


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.argusapp.databinding.ActivityCitizenMainBinding
import com.example.argusapp.data.model.Report
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.citizen.adapters.ReportsAdapter

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var reportsAdapter: ReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitizenMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Встановлення toolbar
        setSupportActionBar(binding.toolbar)

        // Налаштування RecyclerView
        setupRecyclerView()

        // Кнопка для створення нової заявки
        binding.fabAddReport.setOnClickListener {
            startActivity(Intent(this, CreateReportActivity::class.java))
        }

        // Завантаження заявок користувача
        loadReports()
    }

    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter { report ->
            // Обробка кліку на заявці
            val intent = Intent(this, ReportDetailsActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }

        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(this@CitizenMainActivity)
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

        db.collection("reports")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_citizen, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, CitizenProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}