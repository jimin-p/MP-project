package com.example.mp2026.data

import com.example.mp2026.Academy
import kotlin.math.*

data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

enum class AcademySort { NAME, RATING, REVIEW_COUNT, DISTANCE, TUITION }

data class AcademySearch(
    val keyword: String = "",
    val category: String = "전체",
    val maxTuition: Int? = null,
    val minRating: Double = 0.0,
    val radiusKm: Double? = null,
    val origin: Coordinates? = null,
    val sort: AcademySort = AcademySort.NAME
) {
    init {
        require(maxTuition == null || maxTuition >= 0)
        require(minRating.isFinite() && minRating in 0.0..5.0)
        require(radiusKm == null || (radiusKm.isFinite() && radiusKm > 0))
    }

    fun applyTo(academies: List<Academy>): List<Academy> {
        val words = keyword.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val filtered = academies.filter { academy ->
            val text = "${academy.name} ${academy.address} ${academy.curriculum} ${academy.category}"
            (category == "전체" || academy.category == normalizeCategory(category)) &&
                words.all { text.contains(it, ignoreCase = true) } &&
                (maxTuition == null || academy.tuition <= maxTuition) &&
                academy.rating >= minRating &&
                (radiusKm == null || (origin != null && distanceKm(origin, academy) <= radiusKm))
        }
        val comparator = when (sort) {
            AcademySort.NAME -> compareBy<Academy> { it.name }
            AcademySort.RATING -> compareByDescending { it.rating }
            AcademySort.REVIEW_COUNT -> compareByDescending { it.reviewCount }
            AcademySort.TUITION -> compareBy { it.tuition }
            AcademySort.DISTANCE -> compareBy { origin?.let { point -> distanceKm(point, it) } ?: Double.MAX_VALUE }
        }
        return filtered.sortedWith(comparator.thenBy { it.name }.thenBy { it.id })
    }

    companion object {
        fun normalizeCategory(category: String) = if (category == "교육") "입시교육" else category

        fun distanceKm(origin: Coordinates, academy: Academy): Double {
            val lat1 = Math.toRadians(origin.latitude)
            val lat2 = Math.toRadians(academy.latitude)
            val deltaLat = lat2 - lat1
            val deltaLon = Math.toRadians(academy.longitude - origin.longitude)
            val a = (sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2))
                .coerceIn(0.0, 1.0)
            return 6371.0088 * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
