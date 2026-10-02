package com.hanyz.stopme.util

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    // Rumus Haversine untuk menghitung jarak akurat dalam meter
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radius bumi dalam meter
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Format jarak: misal "450 m" atau "2.4 km"
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format(Locale("id", "ID"), "%.1f km", meters / 1000.0)
        }
    }

    // Format ETA detik ke menit
    fun formatEta(seconds: Int): String {
        val minutes = (seconds + 59) / 60
        return if (minutes <= 1) {
            "1 mnt"
        } else if (minutes < 60) {
            "$minutes mnt"
        } else {
            val hours = minutes / 60
            val remMin = minutes % 60
            "${hours} j ${remMin} mnt"
        }
    }
}
