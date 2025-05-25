package com.example.argusapp.data.model


import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import java.util.Date

class TypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.seconds
    }

    @TypeConverter
    fun toTimestamp(seconds: Long?): Timestamp? {
        return seconds?.let { Timestamp(it, 0) }
    }

    @TypeConverter
    fun fromGeoPoint(geoPoint: GeoPoint?): String? {
        return geoPoint?.let { "${it.latitude},${it.longitude}" }
    }

    @TypeConverter
    fun toGeoPoint(value: String?): GeoPoint? {
        return value?.split(",")?.let {
            if (it.size == 2) {
                try {
                    GeoPoint(it[0].toDouble(), it[1].toDouble())
                } catch (e: NumberFormatException) {
                    null
                }
            } else null
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}