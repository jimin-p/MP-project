package com.example.mp2026

import com.example.mp2026.data.*
import org.junit.Assert.*
import org.junit.Test

class AcademySearchTest {
    private fun academy(id: String, category: String = "입시교육", tuition: Int = 200000,
                        rating: Double = 4.0, latitude: Double = 37.5, longitude: Double = 126.95,
                        reviews: Int = 0) = Academy("${id} 수학학원", category, "서울 동작구 상도동", latitude,
        longitude, tuition, rating, "중등 수학", "월 ${tuition}원", "", id, reviews)

    @Test fun combinedFiltersAndEducationAlias() {
        val first = academy("a", rating = 4.8)
        val input = listOf(first, academy("b", tuition = 500000), academy("c", category = "어학"), academy("d", rating = 3.0))
        assertEquals(listOf(first), AcademySearch(keyword = "상도 수학", category = "교육", maxTuition = 250000, minRating = 4.5).applyTo(input))
    }
    @Test fun keywordMatchesAllTokensRegardlessOfCase() {
        assertEquals(1, AcademySearch(keyword = "  A   동작구 ").applyTo(listOf(academy("a"))).size)
        assertTrue(AcademySearch(keyword = "상도 피아노").applyTo(listOf(academy("a"))).isEmpty())
    }
    @Test fun zeroTuitionAndBoundaryRatingAreIncluded() {
        val free = academy("무료", tuition = 0, rating = 5.0)
        assertEquals(listOf(free), AcademySearch(maxTuition = 0, minRating = 5.0).applyTo(listOf(free, academy("유료"))))
    }
    @Test fun distanceAtSamePointAndEquatorAreAccurate() {
        assertEquals(0.0, AcademySearch.distanceKm(Coordinates(37.5,126.95), academy("a")), 0.00001)
        assertEquals(111.195, AcademySearch.distanceKm(Coordinates(0.0, 0.0), academy("a", latitude = 0.0, longitude = 1.0)), 0.01)
    }
    @Test fun radiusAndDistanceSortUseOrigin() {
        val near = academy("b")
        val far = academy("a", latitude = 38.0)
        val query = AcademySearch(origin = Coordinates(37.5, 126.95), radiusKm = 1.0, sort = AcademySort.DISTANCE)
        assertEquals(listOf(near), query.applyTo(listOf(far, near)))
        assertEquals(listOf(near, far), query.copy(radiusKm = null).applyTo(listOf(far, near)))
    }
    @Test fun missingLocationDoesNotInventDistances() {
        assertTrue(AcademySearch(radiusKm = 1.0).applyTo(listOf(academy("a"))).isEmpty())
        assertEquals(listOf("a", "b"), AcademySearch(sort = AcademySort.DISTANCE).applyTo(listOf(academy("b"), academy("a"))).map { it.id })
    }
    @Test fun sortUsesRealReviewCountsAndDeterministicTies() {
        val input = listOf(academy("b", reviews = 2), academy("c", reviews = 5), academy("a", reviews = 2))
        assertEquals(listOf("c", "a", "b"), AcademySearch(sort = AcademySort.REVIEW_COUNT).applyTo(input).map { it.id })
    }
    @Test fun tuitionAndRatingSortDirections() {
        val input = listOf(academy("a", tuition = 500, rating = 3.0), academy("b", tuition = 100, rating = 5.0))
        assertEquals("b", AcademySearch(sort = AcademySort.TUITION).applyTo(input).first().id)
        assertEquals("b", AcademySearch(sort = AcademySort.RATING).applyTo(input).first().id)
    }
    @Test fun invalidSearchInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { AcademySearch(maxTuition = -1) }
        assertThrows(IllegalArgumentException::class.java) { AcademySearch(minRating = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { AcademySearch(radiusKm = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { Coordinates(91.0, 0.0) }
    }
}
