package com.example.mp2026.data

import android.content.Context

class SearchPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("academy_search", Context.MODE_PRIVATE)
    fun load(): AcademySearch = runCatching {
        AcademySearch(
            keyword = preferences.getString("keyword", "").orEmpty(),
            category = preferences.getString("category", "전체").orEmpty(),
            maxTuition = preferences.getInt("maxTuition", -1).takeIf { it >= 0 },
            minRating = preferences.getString("minRating", "0")!!.toDouble(),
            radiusKm = preferences.getString("radiusKm", null)?.toDouble(),
            origin = preferences.getString("latitude", null)?.let { latitude ->
                Coordinates(latitude.toDouble(), preferences.getString("longitude", null)!!.toDouble())
            },
            sort = AcademySort.valueOf(preferences.getString("sort", AcademySort.NAME.name)!!)
        )
    }.getOrDefault(AcademySearch())

    fun save(search: AcademySearch) {
        preferences.edit()
            .putString("keyword", search.keyword).putString("category", search.category)
            .putInt("maxTuition", search.maxTuition ?: -1)
            .putString("minRating", search.minRating.toString())
            .putString("radiusKm", search.radiusKm?.toString())
            .putString("latitude", search.origin?.latitude?.toString())
            .putString("longitude", search.origin?.longitude?.toString())
            .putString("sort", search.sort.name).apply()
    }
}
