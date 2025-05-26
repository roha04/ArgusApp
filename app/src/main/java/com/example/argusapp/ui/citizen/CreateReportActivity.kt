package com.example.argusapp.ui.citizen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ActivityCreateReportBinding
import com.example.argusapp.ui.citizen.adapters.ImagePreviewAdapter
import com.example.argusapp.utils.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import java.io.ByteArrayOutputStream
import java.util.UUID

class CreateReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateReportBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationHelper: LocationHelper
    private lateinit var imageAdapter: ImagePreviewAdapter

    private var selectedImages = mutableListOf<Uri>()
    private var currentLocation: GeoPoint? = null
    private var currentAddress: String = ""
    private var currentUser: User? = null

    // Результати запусків активностей для вибору зображень
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                // Обрано кілька зображень
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    if (selectedImages.size < MAX_IMAGES) {
                        selectedImages.add(clipData.getItemAt(i).uri)
                    } else {
                        showMessage("Можна додати максимум $MAX_IMAGES зображень")
                        break
                    }
                }
            } else if (data?.data != null) {
                // Обрано одне зображення
                if (selectedImages.size < MAX_IMAGES) {
                    selectedImages.add(data.data!!)
                } else {
                    showMessage("Можна додати максимум $MAX_IMAGES зображень")
                }
            }
            updateImagePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Налаштування Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationHelper = LocationHelper(this)

        setupImageAdapter()
        setupCategorySpinner()
        setupUrgencySpinner()
        loadCurrentUser()
        setupListeners()
    }

    private fun setupImageAdapter() {
        imageAdapter = ImagePreviewAdapter(
            onRemoveClicked = { position ->
                selectedImages.removeAt(position)
                updateImagePreview()
            }
        )
        binding.recyclerViewImages.adapter = imageAdapter
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Виберіть категорію",
            "Крадіжка",
            "Вандалізм",
            "Порушення ПДР",
            "Шум",
            "Інше"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupUrgencySpinner() {
        val urgencyLevels = listOf(
            "Середній",
            "Низький",
            "Високий"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, urgencyLevels)
        binding.spinnerUrgency.adapter = adapter
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentUser = document.toObject(User::class.java)
                        currentUser?.id = document.id
                    }
                }
        }
    }

    private fun setupListeners() {
        // Кнопка додавання зображень
        binding.buttonAddImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImageLauncher.launch(intent)
        }

        // Кнопка отримання поточного місцезнаходження
        binding.buttonGetLocation.setOnClickListener {
            checkLocationPermission()
        }

        // Кнопка відправлення заявки
        binding.buttonSubmitReport.setOnClickListener {
            if (validateForm()) {
                submitReport()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запит дозволу, якщо він не наданий
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            // Дозвіл вже є, отримуємо місцезнаходження
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                showMessage("Для отримання місцезнаходження потрібен дозвіл")
            }
        }
    }

    private fun getCurrentLocation() {
        showLoading(true)
        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                currentLocation = GeoPoint(location.latitude, location.longitude)
                // Отримуємо адресу за координатами
                locationHelper.getAddressFromLocation(
                    location.latitude,
                    location.longitude,
                    onSuccess = { address ->
                        showLoading(false)
                        currentAddress = address
                        binding.textViewAddress.text = address
                        binding.textViewAddress.visibility = View.VISIBLE
                    },
                    onError = {
                        showLoading(false)
                        showMessage("Не вдалося отримати адресу")
                    }
                )
            },
            onError = { error ->
                showLoading(false)
                showMessage("Помилка отримання місцезнаходження: $error")
            }
        )
    }

    private fun updateImagePreview() {
        imageAdapter.submitList(selectedImages.toList())
        binding.recyclerViewImages.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
        binding.textViewNoImages.visibility = if (selectedImages.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Перевірка заголовка
        val title = binding.editTextTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.textInputLayoutTitle.error = "Введіть заголовок"
            isValid = false
        } else {
            binding.textInputLayoutTitle.error = null
        }

        // Перевірка опису
        val description = binding.editTextDescription.text.toString().trim()
        if (description.isEmpty()) {
            binding.textInputLayoutDescription.error = "Введіть опис проблеми"
            isValid = false
        } else {
            binding.textInputLayoutDescription.error = null
        }

        // Перевірка категорії
        if (binding.spinnerCategory.selectedItemPosition == 0) {
            showMessage("Виберіть категорію")
            isValid = false
        }

        // Перевірка адреси/місцезнаходження
        if (currentLocation == null || currentAddress.isEmpty()) {
            showMessage("Вкажіть місцезнаходження")
            isValid = false
        }

        return isValid
    }

    private fun submitReport() {
        showLoading(true)

        // Спочатку завантажимо зображення, якщо вони є
        if (selectedImages.isNotEmpty()) {
            uploadImages { imageUrls ->
                createReport(imageUrls)
            }
        } else {
            createReport(emptyList())
        }
    }

    private fun uploadImages(onComplete: (List<String>) -> Unit) {
        val imageUrls = mutableListOf<String>()
        val failedUploads = mutableListOf<Int>()
        var completedCount = 0
        val totalImages = selectedImages.size

        if (selectedImages.isEmpty()) {
            onComplete(emptyList())
            return
        }

        // Check total size before uploading
        if (!validateImagesSize()) {
            showLoading(false)
            return
        }

        // Show progress information
        binding.textViewProgress.visibility = View.VISIBLE
        binding.textViewProgress.text = "Завантаження зображень (0/$totalImages)"

        selectedImages.forEachIndexed { index, uri ->
            try {
                // Create a unique filename with timestamp and index
                val timestamp = System.currentTimeMillis()
                val imageName = "report_${timestamp}_${index}_${UUID.randomUUID()}.jpg"
                val imageRef = storage.reference.child("report_images/$imageName")

                // Compress and resize the image
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Skip if bitmap couldn't be created
                if (originalBitmap == null) {
                    failedUploads.add(index)
                    completedCount++
                    checkUploadCompletion(completedCount, totalImages, imageUrls, failedUploads, onComplete)
                    return@forEachIndexed
                }

                // Resize if necessary
                val maxDimension = 1200 // Maximum width or height
                val scaledBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                    val scale = maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * scale).toInt(),
                        (originalBitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    originalBitmap
                }

                // Compress to JPEG
                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val compressedData = baos.toByteArray()

                // Add metadata
                val metadata = storageMetadata {
                    contentType = "image/jpeg"
                    setCustomMetadata("reportCreator", auth.currentUser?.uid ?: "")
                    setCustomMetadata("createdAt", Timestamp.now().seconds.toString())
                    setCustomMetadata("originalFilename", getFileName(uri) ?: "unknown")
                }

                // Upload the compressed image with metadata
                val uploadTask = imageRef.putBytes(compressedData, metadata)

                // Add progress listener
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    binding.textViewProgress.text = "Завантаження зображення ${index + 1}/$totalImages ($progress%)"
                }

                // Handle success
                uploadTask.addOnSuccessListener {
                    // Free memory
                    if (originalBitmap != scaledBitmap) {
                        scaledBitmap.recycle()
                    }
                    originalBitmap.recycle()

                    // Get download URL
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        imageUrls.add(downloadUrl.toString())
                        completedCount++

                        binding.textViewProgress.text = "Завантаження зображень ($completedCount/$totalImages)"
                        checkUploadCompletion(completedCount, totalImages, imageUrls, failedUploads, onComplete)
                    }.addOnFailureListener { e ->
                        Log.e("UploadImages", "Failed to get download URL: ${e.message}")
                        failedUploads.add(index)
                        completedCount++
                        checkUploadCompletion(completedCount, totalImages, imageUrls, failedUploads, onComplete)
                    }
                }.addOnFailureListener { e ->
                    Log.e("UploadImages", "Failed to upload image $index: ${e.message}")
                    failedUploads.add(index)
                    completedCount++
                    checkUploadCompletion(completedCount, totalImages, imageUrls, failedUploads, onComplete)
                }
            } catch (e: Exception) {
                Log.e("UploadImages", "Exception processing image $index: ${e.message}")
                failedUploads.add(index)
                completedCount++
                checkUploadCompletion(completedCount, totalImages, imageUrls, failedUploads, onComplete)
            }
        }
    }

    private fun checkUploadCompletion(
        completedCount: Int,
        totalCount: Int,
        imageUrls: List<String>,
        failedUploads: List<Int>,
        onComplete: (List<String>) -> Unit
    ) {
        if (completedCount == totalCount) {
            binding.textViewProgress.visibility = View.GONE

            if (failedUploads.isNotEmpty()) {
                val failureMessage = if (failedUploads.size == totalCount) {
                    "Не вдалося завантажити жодне зображення"
                } else {
                    "Не вдалося завантажити ${failedUploads.size} з $totalCount зображень"
                }
                showMessage(failureMessage)
            }

            // Continue with available images
            if (imageUrls.isNotEmpty()) {
                onComplete(imageUrls)
            } else {
                showLoading(false)
                showMessage("Не вдалося завантажити жодне зображення")
            }
        }
    }

    private fun validateImagesSize(): Boolean {
        var totalSize = 0L

        for (uri in selectedImages) {
            try {
                val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                val fileSize = fileDescriptor?.statSize ?: 0
                totalSize += fileSize
                fileDescriptor?.close()
            } catch (e: Exception) {
                Log.e("UploadImages", "Error checking file size: ${e.message}")
            }
        }

        val maxSizeMB = 20
        val maxSizeBytes = maxSizeMB * 1024 * 1024

        if (totalSize > maxSizeBytes) {
            showMessage("Загальний розмір зображень перевищує $maxSizeMB МБ")
            return false
        }

        return true
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    private fun createReport(imageUrls: List<String>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            showMessage("Помилка аутентифікації")
            return
        }

        // Отримання значень з форми
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        // Конвертація вибраної категорії в код
        val categoryPosition = binding.spinnerCategory.selectedItemPosition
        val categoryCode = when (categoryPosition) {
            1 -> "theft"
            2 -> "vandalism"
            3 -> "traffic"
            4 -> "noise"
            else -> "other"
        }

        // Конвертація вибраної терміновості в код
        val urgencyPosition = binding.spinnerUrgency.selectedItemPosition
        val urgencyCode = when (urgencyPosition) {
            0 -> "medium"
            1 -> "low"
            2 -> "high"
            else -> "medium"
        }

        // Створення об'єкта заявки
        val report = Report(
            userId = userId,
            title = title,
            description = description,
            status = "new",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            location = currentLocation,
            address = currentAddress,
            category = categoryCode,
            urgency = urgencyCode,
            imageUrls = imageUrls,
            userDisplayName = currentUser?.displayName ?: auth.currentUser?.displayName ?: "Невідомо"
        )

        // Збереження заявки в Firestore
        db.collection("reports").add(report)
            .addOnSuccessListener {
                showLoading(false)

                // Показуємо діалог успіху
                AlertDialog.Builder(this)
                    .setTitle("Заявку створено")
                    .setMessage("Вашу заявку успішно зареєстровано. Ви можете відстежувати її статус в списку ваших заявок.")
                    .setPositiveButton("OK") { _, _ ->
                        // Повертаємося до головного екрану
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showMessage("Помилка створення заявки: ${e.message}")
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSubmitReport.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Запитуємо підтвердження виходу, якщо форма заповнена
            if (isFormFilled()) {
                AlertDialog.Builder(this)
                    .setTitle("Скасувати створення?")
                    .setMessage("Всі введені дані будуть втрачені. Ви впевнені?")
                    .setPositiveButton("Так") { _, _ -> finish() }
                    .setNegativeButton("Ні", null)
                    .show()
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isFormFilled(): Boolean {
        return binding.editTextTitle.text.toString().isNotEmpty() ||
                binding.editTextDescription.text.toString().isNotEmpty() ||
                selectedImages.isNotEmpty() ||
                currentLocation != null
    }

    private fun handleNavigateUp() {
        if (isFormFilled()) {
            AlertDialog.Builder(this)
                .setTitle("Скасувати створення?")
                .setMessage("Всі введені дані будуть втрачені. Ви впевнені?")
                .setPositiveButton("Так") { _, _ -> finish() }
                .setNegativeButton("Ні", null)
                .show()
        } else {
            finish()
        }
    }



    override fun onBackPressed() {
        // Check if form is filled
        if (isFormFilled()) {
            AlertDialog.Builder(this)
                .setTitle("Скасувати створення?")
                .setMessage("Всі введені дані будуть втрачені. Ви впевнені?")
                .setPositiveButton("Так") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Ні", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val MAX_IMAGES = 5
    }
}