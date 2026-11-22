package com.example.mp2026.data

import com.example.mp2026.Academy

/** Keeps Firestore schema concerns out of Activities and the existing adapter. */
object AcademyMapper {
    fun fromFields(id: String, fields: Map<String, Any>): Academy {
        fun text(key: String): String = (fields[key] as? String)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("$id: $key 누락")
        fun number(key: String): Double = (fields[key] as? Number)?.toDouble()
            ?.takeIf { it.isFinite() } ?: throw IllegalArgumentException("$id: $key 숫자 오류")
        fun integer(key: String, fallback: Int? = null): Int {
            if (fields[key] == null && fallback != null) return fallback
            val value = number(key)
            require(value >= 0 && value <= Int.MAX_VALUE && value % 1 == 0.0) { "$id: $key 범위 오류" }
            return value.toInt()
        }
        val latitude = number("latitude")
        val longitude = number("longitude")
        Coordinates(latitude, longitude)
        val rating = number("rating")
        require(rating in 0.0..5.0) { "$id: rating 범위 오류" }
        val tuition = integer("tuition")
        return Academy(
            name = text("name"), category = AcademySearch.normalizeCategory(text("category")),
            address = text("address"), latitude = latitude, longitude = longitude,
            tuition = tuition, rating = rating, curriculum = text("curriculum"),
            price = (fields["price"] as? String) ?: (fields["가격"] as? String) ?: "월 ${tuition}원",
            image = (fields["image"] as? String) ?: (fields["이미지"] as? String).orEmpty(),
            id = id, reviewCount = integer("reviewCount", 0)
        )
    }
}
