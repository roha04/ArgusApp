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

            // Create location request
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(true)
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

            // First try last location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("LocationHelper", "Using last location")
                        onSuccess(location)
                    } else {
                        // Request fresh location
                        Log.d("LocationHelper", "Requesting fresh location")
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationHelper", "Error: ${e.message}")
                    onError("Помилка: ${e.message}")
                }

        } catch (e: Exception) {
            Log.e("LocationHelper", "Exception: ${e.message}")
            onError("Exception: ${e.message}")
        }
    }
//    fun getAddressFromLocation(
//        latitude: Double,
//        longitude: Double,
//        onSuccess: (String) -> Unit,
//        onError: (String) -> Unit
//    ) {
//        try {
//            val geocoder = Geocoder(context, Locale.getDefault())
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                // Android 13 і вище
//                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
//                    if (addresses.isNotEmpty()) {
//                        val address = formatAddress(addresses.first())
//                        onSuccess(address)
//                    } else {
//                        onError("Не вдалося визначити адресу")
//                    }
//                }
//            } else {
//                // Android 12 і нижче
//                @Suppress("DEPRECATION")
//                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
//                if (!addresses.isNullOrEmpty()) {
//                    val address = formatAddress(addresses.first())
//                    onSuccess(address)
//                } else {
//                    onError("Не вдалося визначити адресу")
//                }
//            }
//        } catch (e: Exception) {
//            onError("Помилка при визначенні адреси: ${e.message}")
//        }
//    }
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