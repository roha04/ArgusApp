package com.example.argusapp.ui.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.databinding.ActivityMapBinding
import com.example.argusapp.ui.police.PoliceReportDetailActivity
import com.example.argusapp.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper

    private val reports = mutableListOf<Report>()
    private val allMarkers = mutableMapOf<String, Marker>()
    private var currentLocationMarker: Marker? = null

    // Filter states - only 3 statuses
    private var showNew = true
    private var showInProgress = true
    private var showResolved = true
    private var userId: String = ""

    private val locationPermissionCode = 101
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MapActivity", "=== onCreate started ===")

        // OSMDroid configuration
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        // Initialize binding
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""

        // Initialize location helper
        locationHelper = LocationHelper(this)

        Log.d("MapActivity", "User ID: $userId")

        // Check permissions
        checkPermissions()

        // Initialize map
        setupMap()

        // Setup filter chips
        setupFilterChips()

        Log.d("MapActivity", "=== onCreate completed ===")
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                locationPermissionCode
            )
        }
    }

    private fun setupMap() {
        Log.d("MapActivity", "Setting up map...")
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(12.0)

        val kyivPoint = GeoPoint(50.4501, 30.5234)
        mapController.setCenter(kyivPoint)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getAndShowCurrentLocation()
        }

        loadReports()
    }

    private fun getAndShowCurrentLocation() {
        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)

                currentLocationMarker?.let { marker ->
                    mapView.overlays.remove(marker)
                }

                val marker = Marker(mapView)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Моє місцезнаходження"

                val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_my_location, null)
                if (icon != null) {
                    marker.icon = icon
                }

                mapView.overlays.add(marker)
                currentLocationMarker = marker
                mapView.invalidate()
            },
            onError = { errorMessage ->
                Log.e("MapActivity", "Location error: $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupFilterChips() {
        Log.d("MapActivity", "=== Setting up filter chips ===")

        try {
            Log.d("MapActivity", "chipNew found: ${binding.chipNew != null}")
            Log.d("MapActivity", "chipInProgress found: ${binding.chipInProgress != null}")
            Log.d("MapActivity", "chipCompleted found: ${binding.chipCompleted != null}")

            // Force all chips to be checked initially
            binding.chipNew.isChecked = true
            binding.chipInProgress.isChecked = true
            binding.chipCompleted.isChecked = true

            // Make chips visible
            binding.chipNew.visibility = View.VISIBLE
            binding.chipInProgress.visibility = View.VISIBLE
            binding.chipCompleted.visibility = View.VISIBLE

            Log.d("MapActivity", "All chips set to visible and checked")

            // Sync variables
            showNew = binding.chipNew.isChecked
            showInProgress = binding.chipInProgress.isChecked
            showResolved = binding.chipCompleted.isChecked

            Log.d("MapActivity", "Initial states - showNew: $showNew, showInProgress: $showInProgress, showResolved: $showResolved")

            // Set up listeners for 3 chips
            binding.chipNew.setOnCheckedChangeListener { _, isChecked ->
                Log.d("MapActivity", "=== chipNew clicked: $isChecked ===")
                showNew = isChecked

                // Prevent all filters from being unchecked
                if (!showNew && !showInProgress && !showResolved) {
                    Log.d("MapActivity", "Preventing all filters from being unchecked")
                    binding.chipNew.isChecked = true
                    showNew = true
                    Toast.makeText(this, "Принаймні один фільтр має бути активним", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                Log.d("MapActivity", "showNew updated to: $showNew")
                updateMarkersVisibility()
            }

            binding.chipInProgress.setOnCheckedChangeListener { _, isChecked ->
                Log.d("MapActivity", "=== chipInProgress clicked: $isChecked ===")
                showInProgress = isChecked

                // Prevent all filters from being unchecked
                if (!showNew && !showInProgress && !showResolved) {
                    Log.d("MapActivity", "Preventing all filters from being unchecked")
                    binding.chipInProgress.isChecked = true
                    showInProgress = true
                    Toast.makeText(this, "Принаймні один фільтр має бути активним", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                Log.d("MapActivity", "showInProgress updated to: $showInProgress")
                updateMarkersVisibility()
            }

            binding.chipCompleted.setOnCheckedChangeListener { _, isChecked ->
                Log.d("MapActivity", "=== chipCompleted clicked: $isChecked ===")
                showResolved = isChecked

                // Prevent all filters from being unchecked
                if (!showNew && !showInProgress && !showResolved) {
                    Log.d("MapActivity", "Preventing all filters from being unchecked")
                    binding.chipCompleted.isChecked = true
                    showResolved = true
                    Toast.makeText(this, "Принаймні один фільтр має бути активним", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                Log.d("MapActivity", "showResolved updated to: $showResolved")
                updateMarkersVisibility()
            }

            Log.d("MapActivity", "=== Filter chips setup completed ===")

        } catch (e: Exception) {
            Log.e("MapActivity", "Error setting up chips: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadReports() {
        Log.d("MapActivity", "=== Loading reports ===")
        binding.progressBar.visibility = View.VISIBLE
        reports.clear()

        db.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                Log.d("MapActivity", "Loaded ${documents.size()} reports from Firestore")

                val isPoliceMode = intent.getBooleanExtra("IS_POLICE_MODE", false)
                Log.d("MapActivity", "Police mode: $isPoliceMode")

                var addedCount = 0
                for (document in documents) {
                    try {
                        val report = document.toObject(Report::class.java)
                        report.id = document.id

                        val shouldAdd = if (isPoliceMode) {
                            report.assignedToId == userId
                        } else {
                            true
                        }

                        if (shouldAdd) {
                            reports.add(report)
                            addedCount++
                            Log.d("MapActivity", "Report ${addedCount}: ID=${report.id}, status=${report.status}, title=${report.title}")
                        }
                    } catch (e: Exception) {
                        Log.e("MapActivity", "Error parsing report: ${e.message}")
                    }
                }

                Log.d("MapActivity", "Added $addedCount reports to list")

                // Count reports by status
                val statusCounts = reports.groupBy { it.status }.mapValues { it.value.size }
                Log.d("MapActivity", "Report status counts: $statusCounts")

                debugReports()
                createAllMarkers()
                updateMarkersVisibility()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("MapActivity", "Error loading reports: ${e.message}")
                Toast.makeText(this, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun debugReports() {
        Log.d("MapActivity", "=== DEBUG: All loaded reports ===")
        for ((index, report) in reports.withIndex()) {
            Log.d("MapActivity", "Report $index:")
            Log.d("MapActivity", "  ID: ${report.id}")
            Log.d("MapActivity", "  Title: ${report.title}")
            Log.d("MapActivity", "  Status: ${report.status}")
            Log.d("MapActivity", "  Location (GeoPoint): ${report.location}")
            Log.d("MapActivity", "  Latitude (property): ${report.latitude}")
            Log.d("MapActivity", "  Longitude (property): ${report.longitude}")
            Log.d("MapActivity", "  Address: ${report.address}")
            Log.d("MapActivity", "  HasLocation(): ${report.hasLocation()}")
            Log.d("MapActivity", "  ---")
        }
        Log.d("MapActivity", "=== END DEBUG ===")
    }

    private fun createAllMarkers() {
        Log.d("MapActivity", "=== Creating all markers ===")
        allMarkers.clear()

        if (reports.isEmpty()) {
            Log.d("MapActivity", "No reports to create markers for")
            return
        }

        var createdCount = 0
        for (report in reports) {
            // Try to get coordinates from different possible sources
            val lat: Double?
            val lng: Double?

            when {
                // First try: use location GeoPoint (new format)
                report.location != null -> {
                    lat = report.location!!.latitude
                    lng = report.location!!.longitude
                    Log.d("MapActivity", "Report ${report.id} using GeoPoint location: $lat, $lng")
                }
                // Second try: use separate latitude/longitude fields (old format)
                report.latitude != null && report.longitude != null -> {
                    lat = report.latitude
                    lng = report.longitude
                    Log.d("MapActivity", "Report ${report.id} using separate lat/lng: $lat, $lng")
                }
                // No location data available
                else -> {
                    Log.d("MapActivity", "Report ${report.id} has no location data")
                    continue
                }
            }

            // Validate coordinates
            if (lat == null || lng == null || lat == 0.0 && lng == 0.0) {
                Log.d("MapActivity", "Report ${report.id} has invalid coordinates: lat=$lat, lng=$lng")
                continue
            }

            val geoPoint = GeoPoint(lat, lng)
            val marker = createMarker(report, geoPoint)
            allMarkers[report.id] = marker
            createdCount++

            Log.d("MapActivity", "Created marker $createdCount for report ${report.id} with status: ${report.status} at coordinates: $lat, $lng")
        }

        Log.d("MapActivity", "Created $createdCount markers total")
    }

    private fun updateMarkersVisibility() {
        Log.d("MapActivity", "=== Updating markers visibility ===")
        Log.d("MapActivity", "Filter states - showNew: $showNew, showInProgress: $showInProgress, showResolved: $showResolved")

        // Clear all report markers from map (keep current location marker)
        val overlaysToRemove = mapView.overlays.filter { overlay ->
            overlay != currentLocationMarker
        }
        mapView.overlays.removeAll(overlaysToRemove)
        Log.d("MapActivity", "Removed ${overlaysToRemove.size} overlays from map")

        var visibleCount = 0

        // Add markers that should be visible based on filters
        for (report in reports) {
            val marker = allMarkers[report.id] ?: continue

            val isVisible = when (report.status) {
                "new" -> {
                    Log.d("MapActivity", "Report ${report.id} is 'new', showNew=$showNew")
                    showNew
                }
                "in_progress" -> {
                    Log.d("MapActivity", "Report ${report.id} is 'in_progress', showInProgress=$showInProgress")
                    showInProgress
                }
                "completed", "resolved" -> {
                    Log.d("MapActivity", "Report ${report.id} is 'completed/resolved', showResolved=$showResolved")
                    showResolved
                }
                else -> {
                    Log.d("MapActivity", "Report ${report.id} has unknown status '${report.status}', showing by default")
                    true
                }
            }

            if (isVisible) {
                mapView.overlays.add(marker)
                visibleCount++
                Log.d("MapActivity", "Added marker for report ${report.id} to map")
            } else {
                Log.d("MapActivity", "Skipped marker for report ${report.id} (filtered out)")
            }
        }

        Log.d("MapActivity", "Showing $visibleCount markers out of ${allMarkers.size} total")

        // Don't zoom - just refresh the map
        mapView.invalidate()
        Log.d("MapActivity", "=== Markers visibility update completed ===")
    }

    private fun createMarker(report: Report, geoPoint: GeoPoint): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint

        val categoryName = getCategoryDisplayName(report.category)
        val urgencyName = getUrgencyDisplayName(report.urgency)
        marker.title = report.title

        val snippetBuilder = StringBuilder()
        snippetBuilder.append("Категорія: $categoryName\n")
        snippetBuilder.append("Терміновість: $urgencyName\n")

        val statusText = when (report.status) {
            "new" -> "Новий"
            "in_progress" -> "В обробці"
            "completed", "resolved" -> "Завершений"
            else -> report.status
        }

        val statusDate = when (report.status) {
            "new" -> report.createdAt
            "in_progress" -> report.assignedAt
            "completed", "resolved" -> report.resolvedAt
            else -> null
        }

        if (statusDate != null) {
            snippetBuilder.append("Статус: $statusText (${dateFormat.format(statusDate.toDate())})\n")
        } else {
            snippetBuilder.append("Статус: $statusText\n")
        }

        if (report.hasAddress()) {
            snippetBuilder.append("Адреса: ${report.address}")
        }

        marker.snippet = snippetBuilder.toString()

        // Correct icon mapping based on status
        val iconResId = when {
            report.status == "new" && report.urgency == "high" -> R.drawable.ic_marker_new_urgent
            report.status == "new" -> R.drawable.ic_marker_new  // Should be BLUE
            report.status == "in_progress" -> R.drawable.ic_marker_in_progress  // Should be YELLOW
            report.status == "completed" || report.status == "resolved" -> R.drawable.ic_marker_resolved  // Should be GREEN
            else -> R.drawable.ic_marker_new
        }

        Log.d("MapActivity", "Report ${report.id} status='${report.status}' -> using icon: $iconResId")

        val icon = ResourcesCompat.getDrawable(resources, iconResId, null)
        if (icon != null) {
            marker.icon = icon
        }

        marker.setOnMarkerClickListener { m, _ ->
            val intent = Intent(this, PoliceReportDetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
            true
        }

        return marker
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

    private fun getUrgencyDisplayName(urgency: String): String {
        return when (urgency) {
            "high" -> "Високий"
            "medium" -> "Середній"
            "low" -> "Низький"
            else -> "Невизначений"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                getAndShowCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Дозвіл на використання місцезнаходження відхилено",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}