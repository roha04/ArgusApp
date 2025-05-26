package com.example.argusapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import java.util.concurrent.TimeUnit

class LocationHelper(private val context: Context) {


    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onSuccess: (Location) -> Unit, onError: (String) -> Unit) {
        try {
            Log.d("LocationHelper", "Getting location")

            // Create location request with more lenient settings
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false) // Changed to false to be more lenient
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build()

            // Create location callback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    if (result.locations.isNotEmpty()) {
                        val location = result.locations[0]
                        Log.d("LocationHelper", "Location received: ${location.latitude}, ${location.longitude}")
                        onSuccess(location)
                    } else {
                        onError("Не вдалося отримати місцезнаходження")
                    }
                }
            }

            // Set timeout for location request
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                onError("Час очікування на отримання місцезнаходження вийшов")
            }

            // First try last location
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d("LocationHelper", "Using last location")
                            handler.removeCallbacks(timeoutRunnable) // Remove timeout if we got a location
                            onSuccess(location)
                        } else {
                            // Request fresh location
                            Log.d("LocationHelper", "Requesting fresh location")
                            try {
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    locationCallback,
                                    Looper.getMainLooper()
                                )

                                // Set timeout for 15 seconds
                                handler.postDelayed(timeoutRunnable, 15000)
                            } catch (e: Exception) {
                                Log.e("LocationHelper", "Error requesting location updates: ${e.message}")
                                onError("Помилка запиту місцезнаходження: ${e.message}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationHelper", "Error getting last location: ${e.message}")

                        // Try requesting fresh location as fallback
                        try {
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.getMainLooper()
                            )

                            // Set timeout for 15 seconds
                            handler.postDelayed(timeoutRunnable, 15000)
                        } catch (e2: Exception) {
                            Log.e("LocationHelper", "Error requesting location updates: ${e2.message}")
                            onError("Помилка запиту місцезнаходження: ${e2.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.e("LocationHelper", "Exception getting last location: ${e.message}")
                onError("Помилка: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("LocationHelper", "Exception in getCurrentLocation: ${e.message}")
            onError("Exception: ${e.message}")
        }
    }
fun getAddressFromLocation(
    latitude: Double,
    longitude: Double,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val mainHandler = Handler(Looper.getMainLooper())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                mainHandler.post {
                    if (addresses.isNotEmpty()) {
                        val address = formatAddress(addresses.first())
                        onSuccess(address)
                    } else {
                        onError("Не вдалося визначити адресу")
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            Thread {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                mainHandler.post {
                    if (!addresses.isNullOrEmpty()) {
                        val address = formatAddress(addresses.first())
                        onSuccess(address)
                    } else {
                        onError("Не вдалося визначити адресу")
                    }
                }
            }.start()
        }
    } catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            onError("Помилка при визначенні адреси: ${e.message}")
        }
    }
}

    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Додаємо деталі адреси, якщо вони є
        if (address.thoroughfare != null) addressParts.add(address.thoroughfare)
        if (address.subThoroughfare != null) addressParts.add(address.subThoroughfare)
        if (address.locality != null) addressParts.add(address.locality)
        if (address.adminArea != null) addressParts.add(address.adminArea)
        if (address.countryName != null) addressParts.add(address.countryName)

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            // Якщо не змогли отримати деталі адреси, повертаємо координати
            "Широта: ${address.latitude}, Довгота: ${address.longitude}"
        }
    }
}