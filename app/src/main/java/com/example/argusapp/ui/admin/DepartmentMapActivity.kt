package com.example.argusapp.ui.admin

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.argusapp.R
import com.example.argusapp.data.model.Department
import com.example.argusapp.databinding.ActivityDepartmentMapBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow

class DepartmentMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepartmentMapBinding
    private lateinit var map: MapView
    private val db = FirebaseFirestore.getInstance()
    private val departments = mutableListOf<Department>()
    private var focusDepartmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        binding = ActivityDepartmentMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get focus department ID if provided
        focusDepartmentId = intent.getStringExtra("departmentId")
        if (focusDepartmentId == null) {
            focusDepartmentId = intent.getStringExtra("focusDepartmentId")
        }

        // Set up action bar
        supportActionBar?.apply {
            title = "Department Map"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Initialize map
        map = binding.mapView
        setupMap()

        // Set up refresh button
        binding.fabRefresh.setOnClickListener {
            loadDepartments()
        }

        // Load departments
        loadDepartments()
    }

    private fun setupMap() {
        // Configure map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(6.0)

        // Default location (Ukraine)
        val defaultLocation = GeoPoint(49.0, 31.0)
        map.controller.setCenter(defaultLocation)
    }

    private fun loadDepartments() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("departments")
            .get()
            .addOnSuccessListener { result ->
                departments.clear()
                map.overlays.clear()

                // Close any open info windows
                InfoWindow.closeAllInfoWindowsOn(map)

                for (document in result) {
                    val department = document.toObject(Department::class.java).apply {
                        id = document.id
                    }
                    departments.add(department)

                    // Add marker for department
                    addDepartmentToMap(department)
                }

                // If we have a specific department to focus on
                if (focusDepartmentId != null) {
                    val focusDepartment = departments.find { it.id == focusDepartmentId }
                    if (focusDepartment != null && focusDepartment.location != null) {
                        val location = focusDepartment.location!!
                        val point = GeoPoint(location.latitude, location.longitude)
                        map.controller.animateTo(point)
                        map.controller.setZoom(15.0)

                        // Find and open info window for this department
                        map.overlays.filterIsInstance<Marker>().forEach { marker ->
                            if (marker.id == focusDepartmentId) {
                                marker.showInfoWindow()
                            }
                        }
                    }
                }
                // Otherwise, if we have departments, zoom to fit them
                else if (departments.isNotEmpty()) {
                    zoomToFitAllDepartments()
                }

                // Redraw map
                map.invalidate()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading departments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDepartmentToMap(department: Department) {
        val location = department.location
        if (location != null) {
            val geoPoint = GeoPoint(location.latitude, location.longitude)

            // Create standard marker (not custom class)
            val marker = Marker(map)
            marker.position = geoPoint
            marker.title = department.name
            marker.snippet = department.address ?: "No address"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Store department ID in marker
            marker.id = department.id

            // Set custom icon
            val icon = ContextCompat.getDrawable(this, R.drawable.ic_departments)
            marker.icon = icon

            // Create info window
            val infoWindow = DepartmentInfoWindow(map, this, department)
            marker.infoWindow = infoWindow

            // Add click listener that shows info window
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                // Close any other open info windows
                InfoWindow.closeAllInfoWindowsOn(map)

                // Show this marker's info window
                clickedMarker.showInfoWindow()

                // Center map on marker
                map.controller.animateTo(clickedMarker.position)

                true // Return true to indicate we handled the click
            }

            // Add department name below marker
            val textOverlay = DepartmentTextOverlay(map, department.name, geoPoint)
            map.overlays.add(textOverlay)

            map.overlays.add(marker)

            // Add jurisdiction circle if radius is specified
            if (department.jurisdictionRadius > 0) {
                val circle = Polygon()
                val points = mutableListOf<GeoPoint>()

                // Create circle points (approximate with polygon)
                for (i in 0 until 360 step 10) {
                    val radiusInMeters = department.jurisdictionRadius * 1000
                    val lat = location.latitude + (radiusInMeters / 111320 * Math.cos(Math.toRadians(i.toDouble())))
                    val lon = location.longitude + (radiusInMeters / (111320 * Math.cos(Math.toRadians(location.latitude))) * Math.sin(Math.toRadians(i.toDouble())))
                    points.add(GeoPoint(lat, lon))
                }

                circle.points = points
                circle.fillColor = Color.argb(40, 63, 81, 181) // Material blue with transparency
                circle.strokeColor = Color.argb(128, 63, 81, 181)
                circle.strokeWidth = 2f

                map.overlays.add(circle)
            }
        }
    }

    private fun zoomToFitAllDepartments() {
        if (departments.isEmpty()) return

        // Get all valid department locations
        val validDepartments = departments.filter { it.location != null }
        if (validDepartments.isEmpty()) return

        // If there's only one department, zoom to it
        if (validDepartments.size == 1) {
            val department = validDepartments[0]
            val location = department.location!!
            val point = GeoPoint(location.latitude, location.longitude)
            map.controller.animateTo(point)
            map.controller.setZoom(13.0)
            return
        }

        // Find the bounding box that contains all departments
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE

        for (department in validDepartments) {
            val location = department.location!!
            minLat = minOf(minLat, location.latitude)
            maxLat = maxOf(maxLat, location.latitude)
            minLon = minOf(minLon, location.longitude)
            maxLon = maxOf(maxLon, location.longitude)
        }

        // Add some padding
        val padding = 0.1 // degrees
        val boundingBox = BoundingBox(
            maxLat + padding,
            maxLon + padding,
            minLat - padding,
            minLon - padding
        )

        // Zoom to the bounding box
        map.zoomToBoundingBox(boundingBox, true)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Custom overlay for drawing department names under markers
class DepartmentTextOverlay(
    private val mapView: MapView,
    private val text: String,
    private val position: GeoPoint
) : org.osmdroid.views.overlay.Overlay() {

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isFakeBoldText = true
        setShadowLayer(2f, 1f, 1f, Color.WHITE)
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow) return
        if (canvas == null) return

        // Convert geo coordinates to screen coordinates
        val point = mapView?.projection?.toPixels(position, null) ?: return

        // Draw text below the marker position
        canvas.drawText(text, point.x.toFloat(), point.y.toFloat() + 70f, textPaint)
    }
}


