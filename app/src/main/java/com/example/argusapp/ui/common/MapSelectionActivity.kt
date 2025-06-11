package com.example.argusapp.ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.argusapp.databinding.ActivityMapSelectionBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapSelectionBinding
    private lateinit var map: MapView
    private var selectedLocation: GeoPoint? = null
    private var currentMarker: Marker? = null
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        binding = ActivityMapSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up action bar
        supportActionBar?.apply {
            title = "Select Location"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Initialize map
        map = binding.mapView
        setupMap()

        // Set up confirm button
        binding.btnConfirmLocation.setOnClickListener {
            if (selectedLocation != null) {
                val resultIntent = Intent().apply {
                    putExtra("latitude", selectedLocation!!.latitude)
                    putExtra("longitude", selectedLocation!!.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        // Configure map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(6.0)

        // Default location (Ukraine)
        val defaultLocation = GeoPoint(49.0, 31.0)
        map.controller.setCenter(defaultLocation)

        // Add my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        // Add map click events
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    addMarkerAtPosition(it)
                    selectedLocation = it
                    binding.tvSelectedLocation.text = "Selected: Lat: ${it.latitude}, Lng: ${it.longitude}"
                    binding.btnConfirmLocation.isEnabled = true
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
        map.overlays.add(0, mapEventsOverlay)
    }

    private fun addMarkerAtPosition(position: GeoPoint) {
        // Remove previous marker if exists
        currentMarker?.let {
            map.overlays.remove(it)
        }

        // Add new marker
        val marker = Marker(map)
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Selected Location"
        map.overlays.add(marker)

        // Store current marker for later removal
        currentMarker = marker

        // Redraw map
        map.invalidate()
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