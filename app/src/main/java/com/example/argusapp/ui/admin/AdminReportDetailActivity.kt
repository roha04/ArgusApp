package com.example.argusapp.ui.admin


import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.data.model.ReportComment
import com.example.argusapp.data.model.ReportUpdate
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ActivityAdminReportDetailBinding
import com.example.argusapp.ui.common.adapters.CommentsAdapter
import com.example.argusapp.ui.common.adapters.PhotosAdapter
import com.example.argusapp.ui.common.adapters.ReportUpdatesAdapter
import com.example.argusapp.utils.ReportUpdateHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminReportDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var reportId: String
    private var currentReport: Report? = null
    private var currentUser: User? = null

    private lateinit var photosAdapter: PhotosAdapter
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var updatesAdapter: ReportUpdatesAdapter

    private val policeOfficers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштування Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        reportId = intent.getStringExtra("REPORT_ID") ?: ""
        if (reportId.isEmpty()) {
            Toast.makeText(this, "Помилка: Неможливо отримати дані заявки", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerViews()
        loadCurrentUser()
        loadPoliceOfficers()
        loadReportData()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        // Налаштування адаптера для фотографій
        photosAdapter = PhotosAdapter { photoUrl ->
            // Відкриття повноекранного перегляду фото
            // TODO: Реалізувати перегляд фото
        }
        binding.recyclerViewPhotos.adapter = photosAdapter
        binding.recyclerViewPhotos.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Налаштування адаптера для коментарів
        commentsAdapter = CommentsAdapter()
        binding.recyclerViewComments.adapter = commentsAdapter
        binding.recyclerViewComments.layoutManager = LinearLayoutManager(this)

        // Налаштування адаптера для оновлень
        updatesAdapter = ReportUpdatesAdapter()
        binding.recyclerViewUpdates.adapter = updatesAdapter
        binding.recyclerViewUpdates.layoutManager = LinearLayoutManager(this)
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    currentUser?.id = document.id
                }
            }
    }

    private fun loadPoliceOfficers() {
        db.collection("users")
            .whereEqualTo("role", "police")
            .get()
            .addOnSuccessListener { documents ->
                policeOfficers.clear()
                for (document in documents) {
                    val officer = document.toObject(User::class.java)
                    officer.id = document.id
                    policeOfficers.add(officer)
                }

                // Заповнення спіннера
                val policeNames = policeOfficers.map { "${it.displayName} (${it.badgeNumber})" }
                val spinnerAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    policeNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPolice.adapter = spinnerAdapter
            }
    }

    private fun loadReportData() {
        showLoading(true)

        db.collection("reports").document(reportId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val report = document.toObject(Report::class.java)
                    report?.id = document.id
                    currentReport = report

                    displayReportDetails(report)
                    loadReportComments()
                    loadReportUpdates()
                } else {
                    Toast.makeText(this, "Заявку не знайдено", Toast.LENGTH_SHORT).show()
                    finish()
                }

                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayReportDetails(report: Report?) {
        if (report == null) return

        // Заголовок і опис
        binding.textViewReportTitle.text = report.title
        binding.textViewReportDescription.text = report.description

        // Статус
        val (statusText, statusColor) = when (report.status) {
            "new" -> Pair("Новий", R.color.status_new)
            "in_progress" -> Pair("В обробці", R.color.status_in_progress)
            "resolved" -> Pair("Вирішено", R.color.status_resolved)
            else -> Pair("Закрито", R.color.status_closed)
        }
        binding.chipReportStatus.text = statusText
        binding.chipReportStatus.setChipBackgroundColorResource(statusColor)

        // Категорія
        val categoryText = getCategoryDisplayName(report.category)
        binding.chipReportCategory.text = categoryText

        // Терміновість
        val urgencyText = when (report.urgency) {
            "high" -> "Терміновий"
            "medium" -> "Середній"
            "low" -> "Низький"
            else -> "Невідомо"
        }
        binding.chipReportUrgency.text = urgencyText

        // Адреса
        binding.textViewReportAddress.text = report.address

        // Фотографії
        photosAdapter.submitList(report.imageUrls)

        // Заявник
        binding.textViewReporterName.text = report.userDisplayName

        // Встановлення радіокнопки поточного статусу
        when (report.status) {
            "new" -> binding.radioNew.isChecked = true
            "in_progress" -> binding.radioInProgress.isChecked = true
            "resolved" -> binding.radioResolved.isChecked = true
        }

        // TODO: Налаштування контейнера карти
    }

    private fun loadReportComments() {
        db.collection("report_comments")
            .whereEqualTo("reportId", reportId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val comments = documents.toObjects(ReportComment::class.java)
                commentsAdapter.submitList(comments)

                if (comments.isEmpty()) {
                    binding.recyclerViewComments.visibility = View.GONE
                } else {
                    binding.recyclerViewComments.visibility = View.VISIBLE
                }
            }
    }

    private fun loadReportUpdates() {
        db.collection("report_updates")
            .whereEqualTo("reportId", reportId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val updates = documents.toObjects(ReportUpdate::class.java)
                updatesAdapter.submitList(updates)

                if (updates.isEmpty()) {
                    binding.recyclerViewUpdates.visibility = View.GONE
                } else {
                    binding.recyclerViewUpdates.visibility = View.VISIBLE
                }
            }
    }

    private fun setupListeners() {
        // Кнопка призначення поліцейського
        binding.buttonAssign.setOnClickListener {
            val selectedPosition = binding.spinnerPolice.selectedItemPosition
            if (selectedPosition >= 0 && selectedPosition < policeOfficers.size) {
                val selectedOfficer = policeOfficers[selectedPosition]
                assignOfficerToReport(selectedOfficer)
            }
        }

        // Кнопка оновлення статусу
        binding.buttonUpdateStatus.setOnClickListener {
            val newStatus = when {
                binding.radioNew.isChecked -> "new"
                binding.radioInProgress.isChecked -> "in_progress"
                binding.radioResolved.isChecked -> "resolved"
                else -> currentReport?.status ?: "new"
            }

            updateReportStatus(newStatus)
        }

        // Кнопка додавання коментаря
        binding.buttonAddComment.setOnClickListener {
            val commentText = binding.editTextComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addReportComment(commentText)
            } else {
                Toast.makeText(this, "Введіть текст коментаря", Toast.LENGTH_SHORT).show()
            }
        }
    }

private fun assignOfficerToReport(officer: User) {
    val report = currentReport ?: return
    val user = currentUser ?: return

    val reportRef = db.collection("reports").document(reportId)

    // Debug: Log the officer ID being assigned
    Log.d("AdminReport", "Assigning officer ID: ${officer.id} to report: $reportId")

    // Оновлення звіту з інформацією про призначеного офіцера
    val updates = hashMapOf<String, Any>(
        "assignedToId" to officer.id,
        "assignedAt" to Timestamp.now(),
        "isAssigned" to true,
        "updatedAt" to Timestamp.now()
    )

    reportRef.update(updates)
        .addOnSuccessListener {
            Log.d("AdminReport", "Successfully updated report with officer assignment")

            // Створення запису про оновлення
            val update = ReportUpdateHelper.createAssignmentUpdate(
                reportId = reportId,
                user = user,
                assignedTo = officer
            )

            db.collection("report_updates").add(update)
                .addOnSuccessListener {
                    Log.d("AdminReport", "Created assignment update record")

                    Toast.makeText(
                        this,
                        "Офіцера ${officer.displayName} призначено до заявки",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Оновлення даних
                    loadReportData()
                }
                .addOnFailureListener { e ->
                    Log.e("AdminReport", "Failed to create update record: ${e.message}")
                    Toast.makeText(this, "Помилка при створенні запису оновлення: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        .addOnFailureListener { e ->
            Log.e("AdminReport", "Failed to assign officer: ${e.message}")
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

    private fun updateReportStatus(newStatus: String) {
        val report = currentReport ?: return
        val user = currentUser ?: return

        if (report.status == newStatus) {
            Toast.makeText(this, "Статус заявки не змінився", Toast.LENGTH_SHORT).show()
            return
        }

        val reportRef = db.collection("reports").document(reportId)

        // Додаткові оновлення в залежності від статусу
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to Timestamp.now()
        )

        if (newStatus == "resolved") {
            updates["resolvedAt"] = Timestamp.now()
            updates["isResolved"] = true
        }

        reportRef.update(updates)
            .addOnSuccessListener {
                // Створення запису про оновлення
                val update = ReportUpdateHelper.createStatusChangeUpdate(
                    reportId = reportId,
                    user = user,
                    oldStatus = report.status,
                    newStatus = newStatus
                )

                db.collection("report_updates").add(update)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Статус заявки оновлено",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Оновлення даних
                        loadReportData()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addReportComment(commentText: String) {
        val user = currentUser ?: return

        val comment = ReportComment(
            reportId = reportId,
            userId = user.id,
            userDisplayName = user.displayName,
            userRole = user.role,
            text = commentText,
            createdAt = Timestamp.now(),
            isOfficial = user.role != "citizen",
            userPhotoUrl = user.photoUrl
        )

        db.collection("report_comments").add(comment)
            .addOnSuccessListener {
                Toast.makeText(this, "Коментар додано", Toast.LENGTH_SHORT).show()
                binding.editTextComment.text?.clear()

                // Створення оновлення для коментаря адміністратора
                if (user.role == "admin") {
                    val update = ReportUpdateHelper.createOfficialNoteUpdate(
                        reportId = reportId,
                        user = user,
                        note = commentText
                    )

                    db.collection("report_updates").add(update)
                }

                // Оновлення кількості коментарів
                db.collection("reports").document(reportId)
                    .update("commentCount", (currentReport?.commentCount ?: 0) + 1)

                // Оновлення списку коментарів
                loadReportComments()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCategoryDisplayName(categoryId: String): String {
        return when (categoryId) {
            "theft" -> "Крадіжка"
            "vandalism" -> "Вандалізм"
            "traffic" -> "Порушення ПДР"
            "noise" -> "Шум"
            "other" -> "Інше"
            else -> categoryId
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}