package com.example.argusapp.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import org.osmdroid.util.BoundingBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.databinding.ActivityMapBinding
import com.example.argusapp.ui.police.PoliceReportDetailActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var db: FirebaseFirestore
    private lateinit var locationOverlay: MyLocationNewOverlay

    private val reports = mutableListOf<Report>()
    private val markers = mutableMapOf<String, Marker>()
    private val overlays = mutableMapOf<String, Overlay>()

    private var showNew = true
    private var showInProgress = true
    private var showResolved = true

    private val locationPermissionCode = 101
    private val storagePermissionCode = 102

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

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Check storage permissions for OSMDroid cache
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

        // Enable location overlay
        enableMyLocation()

        // Load reports from Firestore
        loadReports()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enable location overlay
            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            locationOverlay.isDrawAccuracyEnabled = true
            mapView.overlays.add(locationOverlay)

            // Move to location when available
            locationOverlay.runOnFirstFix {
                runOnUiThread {
                    val myLocation = locationOverlay.myLocation
                    if (myLocation != null) {
                        mapView.controller.animateTo(GeoPoint(myLocation))
                        locationOverlay.disableFollowLocation() // Disable follow after initial move
                    }
                }
            }
        }
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

        db.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                reports.clear()

                for (document in documents) {
                    try {
                        val report = document.toObject(Report::class.java)
                        report.id = document.id
                        reports.add(report)
                    } catch (e: Exception) {
                        // Skip reports with invalid data
                    }
                }

                // Add markers for reports
                addMarkersToMap()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
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
            return
        }

        // Collect visible points for calculating bounds
        val visiblePoints = mutableListOf<GeoPoint>()

        // Add new markers
        for (report in reports) {
            if (report.latitude == null || report.longitude == null) continue

            val geoPoint = GeoPoint(report.latitude!!, report.longitude!!)

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
        }

        mapView.invalidate()
    }

    private fun createMarker(report: Report, geoPoint: GeoPoint): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.title = report.title
        marker.snippet = report.address

        // Set icon based on status
        val iconResId = when (report.status) {
            "new" -> R.drawable.ic_marker_new
            "in_progress" -> R.drawable.ic_marker_in_progress
            "resolved" -> R.drawable.ic_marker_resolved
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
                enableMyLocation()
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