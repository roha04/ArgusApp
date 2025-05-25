package com.example.argusapp.data.model


import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var address: String = ""
) : Parcelable {
    // Конвертація в GeoPoint для збереження в Firestore
    fun toGeoPoint(): GeoPoint {
        return GeoPoint(latitude, longitude)
    }

    companion object {
        // Створення з GeoPoint
        fun fromGeoPoint(geoPoint: GeoPoint, address: String = ""): Location {
            return Location(geoPoint.latitude, geoPoint.longitude, address)
        }
    }
}