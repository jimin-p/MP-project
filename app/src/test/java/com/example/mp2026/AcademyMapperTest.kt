package com.example.mp2026

import com.example.mp2026.data.AcademyMapper
import org.junit.Assert.*
import org.junit.Test

class AcademyMapperTest {
    private val fields = mapOf<String, Any>("name" to "학원", "category" to "교육", "address" to "서울",
        "latitude" to 37.5, "longitude" to 126.95, "tuition" to 200000L, "rating" to 4.5,
        "curriculum" to "수학", "가격" to "월 200,000원", "이미지" to "https://example.com/image.png")
    @Test fun supportsFirestoreLongsAndLegacyKoreanFieldNames() {
        val result = AcademyMapper.fromFields("academy-a", fields)
        assertEquals(200000, result.tuition)
        assertEquals("academy-a", result.id)
        assertEquals("입시교육", result.category)
        assertEquals("월 200,000원", result.price)
        assertEquals(0, result.reviewCount)
    }
    @Test fun canonicalFieldsTakePrecedence() {
        val result = AcademyMapper.fromFields("a", fields + mapOf("price" to "새 가격", "image" to "새 이미지", "reviewCount" to 3L))
        assertEquals("새 가격", result.price)
        assertEquals("새 이미지", result.image)
        assertEquals(3, result.reviewCount)
    }
    @Test fun corruptFieldsFailInsteadOfFabricatingAnAcademy() {
        for (change in listOf(mapOf("name" to ""), mapOf("rating" to 7), mapOf("latitude" to Double.NaN),
            mapOf("tuition" to 1.5), mapOf("tuition" to Long.MAX_VALUE), mapOf("reviewCount" to -1))) {
            assertThrows(IllegalArgumentException::class.java) { AcademyMapper.fromFields("a", fields + change) }
        }
    }
    @Test fun missingRequiredFieldFails() {
        assertThrows(IllegalArgumentException::class.java) { AcademyMapper.fromFields("a", fields - "curriculum") }
    }
}
