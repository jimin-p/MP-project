package com.example.mp2026

data class Academy(
    val name: String,         // 학원 이름
    val category: String,     // 카테고리 (예: 입시교육)
    val address: String,      // 주소
    val latitude: Double,     // 위도
    val longitude: Double,    // 경도
    val tuition: Int,         // 학원비 숫자
    val rating: Double,       // 별점
    val curriculum: String,   // 커리큘럼 / 과목 설명
    val price: String,        // 가격 텍스트 (예: "월 300,000원")
    val image: String         // 이미지 URL
)