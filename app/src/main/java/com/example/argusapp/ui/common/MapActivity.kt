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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper

    private val reports = mutableListOf<Report>()
    private val markers = mutableMapOf<String, Marker>()
    private val overlays = mutableMapOf<String, Overlay>()

    private var showNew = true
    private var showInProgress = true
    private var showResolved = true
    private var userId: String = ""

    private val locationPermissionCode = 101
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // For API < 30, OSMDroid needs write storage permission
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
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Add zoom controls
        val mapController = mapView.controller
        mapController.setZoom(12.0)

        // Set center to Kyiv by default
        val kyivPoint = GeoPoint(50.4501, 30.5234)
        mapController.setCenter(kyivPoint)

        // Get current location and center map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getAndShowCurrentLocation()
        }

        // Load reports from Firestore
        loadReports()
    }

    private fun getAndShowCurrentLocation() {
        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)

                // Add a marker for current location
                val marker = Marker(mapView)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Моє місцезнаходження"

                // Use a different icon for current location
                val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_my_location, null)
                if (icon != null) {
                    marker.icon = icon
                }

                mapView.overlays.add(marker)
                mapView.invalidate()
            },
            onError = { errorMessage ->
                Log.e("MapActivity", "Location error: $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupFilterChips() {
        binding.chipNew.setOnCheckedChangeListener { _, isChecked ->
            showNew = isChecked
            updateMarkers()
        }

        binding.chipInProgress.setOnCheckedChangeListener { _, isChecked ->
            showInProgress = isChecked
            updateMarkers()
        }

        binding.chipResolved.setOnCheckedChangeListener { _, isChecked ->
            showResolved = isChecked
            updateMarkers()
        }
    }

    private fun loadReports() {
        binding.progressBar.visibility = View.VISIBLE
        reports.clear()

        // Try loading all reports first
        db.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                Log.d("MapActivity", "Loaded ${documents.size()} reports")

                // Check if we're in police mode
                val isPoliceMode = intent.getBooleanExtra("IS_POLICE_MODE", false)

                for (document in documents) {
                    try {
                        val report = document.toObject(Report::class.java)
                        report.id = document.id

                        // Apply filtering based on role
                        val shouldAdd = if (isPoliceMode) {
                            // For police, only show assigned reports
                            report.assignedToId == userId
                        } else {
                            // For others, show all reports
                            true
                        }

                        if (shouldAdd) {
                            reports.add(report)
                            Log.d("MapActivity", "Added report: ${report.id}, title: ${report.title}, " +
                                    "lat: ${report.latitude}, lng: ${report.longitude}")
                        }
                    } catch (e: Exception) {
                        Log.e("MapActivity", "Error parsing report: ${e.message}")
                    }
                }

                // Add markers for reports
                addMarkersToMap()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("MapActivity", "Error loading reports: ${e.message}")
                Toast.makeText(this, "Помилка завантаження даних: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMarkersToMap() {
        // Clear existing markers
        val overlaysToRemove = overlays.values.toList()
        for (overlay in overlaysToRemove) {
            mapView.overlays.remove(overlay)
        }
        markers.clear()
        overlays.clear()

        if (reports.isEmpty()) {
            Log.d("MapActivity", "No reports to display on map")
            Toast.makeText(this, "Немає звітів для відображення", Toast.LENGTH_SHORT).show()
            return
        }

        // Collect visible points for calculating bounds
        val visiblePoints = mutableListOf<GeoPoint>()

        // Add new markers
        for (report in reports) {
            // Use latitude/longitude fields directly
            if (report.latitude == null || report.longitude == null) {
                Log.d("MapActivity", "Report ${report.id} has no lat/lng")
                continue
            }

            val geoPoint = GeoPoint(report.latitude!!, report.longitude!!)
            Log.d("MapActivity", "Creating marker for report ${report.id} at ${report.latitude}, ${report.longitude}")

            // Check if this marker should be visible based on filters
            val isVisible = when (report.status) {
                "new" -> showNew
                "in_progress" -> showInProgress
                "resolved" -> showResolved
                else -> true
            }

            if (isVisible) {
                val marker = createMarker(report, geoPoint)
                markers[report.id] = marker
                mapView.overlays.add(marker)
                overlays[report.id] = marker

                // Add to visible points
                visiblePoints.add(geoPoint)
            }
        }

        // Zoom to show all markers if we have any
        if (visiblePoints.isNotEmpty()) {
            Log.d("MapActivity", "Zooming to ${visiblePoints.size} visible points")

            // Use OSMDroid's utility to create a bounding box from points
            if (visiblePoints.size == 1) {
                // If only one point, center on it
                mapView.controller.animateTo(visiblePoints[0])
                mapView.controller.setZoom(16.0)
            } else {
                // For multiple points, calculate bounds
                val boundingBox = BoundingBox.fromGeoPoints(visiblePoints)
                // Add padding around the bounding box
                mapView.zoomToBoundingBox(boundingBox, true, 100)
            }
        } else {
            Log.d("MapActivity", "No visible points to zoom to")
        }

        mapView.invalidate()
    }

    private fun createMarker(report: Report, geoPoint: GeoPoint): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint

        // Set title with category and urgency info
        val categoryName = getCategoryDisplayName(report.category)
        val urgencyName = getUrgencyDisplayName(report.urgency)
        marker.title = report.title

        // Create a more informative snippet
        val snippetBuilder = StringBuilder()
        snippetBuilder.append("Категорія: $categoryName\n")
        snippetBuilder.append("Терміновість: $urgencyName\n")

        // Add status with date
        val statusText = when (report.status) {
            "new" -> "Новий"
            "in_progress" -> "В обробці"
            "resolved" -> "Вирішений"
            else -> report.status
        }

        val statusDate = when (report.status) {
            "new" -> report.createdAt
            "in_progress" -> report.assignedAt
            "resolved" -> report.resolvedAt
            else -> null
        }

        if (statusDate != null) {
            snippetBuilder.append("Статус: $statusText (${dateFormat.format(statusDate.toDate())})\n")
        } else {
            snippetBuilder.append("Статус: $statusText\n")
        }

        // Add address if available
        if (report.address.isNotEmpty()) {
            snippetBuilder.append("Адреса: ${report.address}")
        }

        marker.snippet = snippetBuilder.toString()

        // Set icon based on status and urgency
        val iconResId = when {
            report.status == "new" && report.urgency == "high" -> R.drawable.ic_marker_new_urgent
            report.status == "new" -> R.drawable.ic_marker_new
            report.status == "in_progress" -> R.drawable.ic_marker_in_progress
            report.status == "resolved" -> R.drawable.ic_marker_resolved
            else -> R.drawable.ic_marker_new
        }

        // Create or find the drawable
        val icon = ResourcesCompat.getDrawable(resources, iconResId, null)
        if (icon != null) {
            marker.icon = icon
        }

        // Set behavior for info window click
        marker.setOnMarkerClickListener { m, _ ->
            // Open report details
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

    private fun updateMarkers() {
        for (report in reports) {
            val overlay = overlays[report.id] ?: continue

            val isVisible = when (report.status) {
                "new" -> showNew
                "in_progress" -> showInProgress
                "resolved" -> showResolved
                else -> true
            }

            if (isVisible) {
                if (!mapView.overlays.contains(overlay)) {
                    mapView.overlays.add(overlay)
                }
            } else {
                mapView.overlays.remove(overlay)
            }
        }

        mapView.invalidate()
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