package com.example.argusapp.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.data.model.ReportComment
import com.example.argusapp.data.model.ReportUpdate
import com.example.argusapp.databinding.ActivityReportDetailsBinding
import com.example.argusapp.ui.common.MapActivity
import com.example.argusapp.ui.common.adapters.CommentsAdapter
import com.example.argusapp.ui.common.adapters.PhotosAdapter
import com.example.argusapp.ui.common.adapters.ReportUpdatesAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var photosAdapter: PhotosAdapter
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var updatesAdapter: ReportUpdatesAdapter
    private var reportId: String = ""
    private var report: Report? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get report ID from intent
        reportId = intent.getStringExtra("REPORT_ID") ?: ""
        if (reportId.isEmpty()) {
            showError("Помилка: ID заявки не вказано")
            finish()
            return
        }

        setupAdapters()
        setupListeners()
        loadReportDetails()
    }

    private fun setupAdapters() {
        // Setup photos adapter
        photosAdapter = PhotosAdapter { url ->
            // Handle photo click - perhaps show full screen
            // Implementation for full screen viewing would go here
        }
        binding.recyclerViewPhotos.apply {
            layoutManager = LinearLayoutManager(this@ReportDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photosAdapter
            setHasFixedSize(true)
        }

        // Setup comments adapter
        commentsAdapter = CommentsAdapter()
        binding.recyclerViewComments.apply {
            layoutManager = LinearLayoutManager(this@ReportDetailsActivity)
            adapter = commentsAdapter
            setHasFixedSize(true)
        }

        // Setup updates adapter
        updatesAdapter = ReportUpdatesAdapter()
        binding.recyclerViewUpdates.apply {
            layoutManager = LinearLayoutManager(this@ReportDetailsActivity)
            adapter = updatesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        // View location on map button
        binding.buttonViewOnMap.setOnClickListener {
            if (report?.location != null) {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("LATITUDE", report?.location?.latitude)
                intent.putExtra("LONGITUDE", report?.location?.longitude)
                intent.putExtra("TITLE", report?.title)
                intent.putExtra("ADDRESS", report?.address)
                startActivity(intent)
            }
        }

        // Send comment button
        binding.buttonSendComment.setOnClickListener {
            val commentText = binding.editTextComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            }
        }
    }

    private fun loadReportDetails() {
        showLoading(true)

        db.collection("reports").document(reportId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    report = document.toObject(Report::class.java)?.apply { id = document.id }
                    displayReportDetails()
                    loadComments()
                    loadUpdates()
                } else {
                    showError("Заявку не знайдено")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Помилка завантаження: ${e.message}")
            }
    }

    private fun displayReportDetails() {
        report?.let { report ->
            // Set title and basic info
            binding.toolbar.title = report.title
            binding.textViewTitle.text = report.title
            binding.textViewDescription.text = report.description
            binding.textViewCategory.text = getCategoryName(report.category)
            binding.textViewUrgency.text = getUrgencyName(report.urgency)
            binding.textViewStatus.text = getStatusName(report.status)
            binding.textViewAddress.text = report.address ?: "Адреса не вказана"

            // Set appropriate status color
            binding.textViewStatus.setBackgroundResource(R.drawable.bg_status)

            // Set date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.textViewDate.text = dateFormat.format(report.createdAt?.toDate() ?: Date())

            // Show/hide map button based on location availability
            binding.buttonViewOnMap.visibility = if (report.location != null) View.VISIBLE else View.GONE

            // Load images
            if (report.imageUrls.isNullOrEmpty()) {
                binding.recyclerViewPhotos.visibility = View.GONE
                binding.textViewNoPhotos.visibility = View.VISIBLE
            } else {
                binding.recyclerViewPhotos.visibility = View.VISIBLE
                binding.textViewNoPhotos.visibility = View.GONE
                photosAdapter.submitList(report.imageUrls)
            }

            showLoading(false)
        }
    }

    private fun loadComments() {
        db.collection("reports").document(reportId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    showError("Помилка завантаження коментарів")
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    val comment = doc.toObject(ReportComment::class.java)
                    comment?.id = doc.id
                    comment
                } ?: emptyList()

                if (comments.isEmpty()) {
                    binding.textViewNoComments.visibility = View.VISIBLE
                } else {
                    binding.textViewNoComments.visibility = View.GONE
                }

                commentsAdapter.submitList(comments)
            }
    }

    private fun loadUpdates() {
        db.collection("reports").document(reportId)
            .collection("updates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    showError("Помилка завантаження оновлень")
                    return@addSnapshotListener
                }

                val updates = snapshot?.documents?.mapNotNull { doc ->
                    val update = doc.toObject(ReportUpdate::class.java)
                    update?.id = doc.id
                    update
                } ?: emptyList()

                if (updates.isEmpty()) {
                    binding.textViewNoUpdates.visibility = View.VISIBLE
                    binding.recyclerViewUpdates.visibility = View.GONE
                    binding.cardViewUpdates.visibility = View.GONE
                } else {
                    binding.textViewNoUpdates.visibility = View.GONE
                    binding.recyclerViewUpdates.visibility = View.VISIBLE
                    binding.cardViewUpdates.visibility = View.VISIBLE
                    updatesAdapter.submitList(updates)
                }
            }
    }

    private fun addComment(commentText: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Користувач"

        val comment = ReportComment(
            userId = userId,
            userDisplayName = userName,
            text = commentText,
            createdAt = Timestamp.now(),
            isOfficial = false
        )

        binding.editTextComment.text?.clear()
        binding.buttonSendComment.isEnabled = false

        db.collection("reports").document(reportId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                binding.buttonSendComment.isEnabled = true
            }
            .addOnFailureListener { e ->
                binding.buttonSendComment.isEnabled = true
                showError("Помилка додавання коментаря: ${e.message}")
            }
    }

    private fun getCategoryName(categoryCode: String): String {
        return when (categoryCode) {
            "theft" -> "Крадіжка"
            "vandalism" -> "Вандалізм"
            "traffic" -> "Порушення ПДР"
            "noise" -> "Шум"
            else -> "Інше"
        }
    }

    private fun getUrgencyName(urgencyCode: String): String {
        return when (urgencyCode) {
            "high" -> "Високий"
            "medium" -> "Середній"
            "low" -> "Низький"
            else -> "Середній"
        }
    }

    private fun getStatusName(statusCode: String): String {
        return when (statusCode) {
            "new" -> "Нова"
            "in_progress" -> "В обробці"
            "completed" -> "Завершена"
            "rejected" -> "Відхилена"
            else -> "Невідомо"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}