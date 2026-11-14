package com.example.mp2026

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class AcademyListActivity : AppCompatActivity() {

    private lateinit var academyAdapter: AcademyAdapter
    private lateinit var fullAcademyList: List<Academy>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_list)

        // 1. assets 폴더의 academies.json 읽어오기
        fullAcademyList = loadAcademiesFromJson()

        // 상단 개수 텍스트 업데이트 (예: "우리 동네 학원 60")
        val tvCount = findViewById<TextView>(R.id.tvAcademyCount)
        tvCount.text = "우리 동네 학원 ${fullAcademyList.size}"

        // 2. RecyclerView 세팅
        val recyclerView = findViewById<RecyclerView>(R.id.rvAcademyList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        academyAdapter = AcademyAdapter(fullAcademyList)
        recyclerView.adapter = academyAdapter

        // 3. 카테고리 버튼 필터링 기능 연결
        val btnAll = findViewById<Button>(R.id.btnCatAll)
        val btnEdu = findViewById<Button>(R.id.btnCatEdu)
        val btnLang = findViewById<Button>(R.id.btnCatLang)
        val btnArt = findViewById<Button>(R.id.btnCatArt)
        val btnOneday = findViewById<Button>(R.id.btnCatOneday)

        btnAll.setOnClickListener { filterCategory("전체") }
        btnEdu.setOnClickListener { filterCategory("입시교육") }
        btnLang.setOnClickListener { filterCategory("어학") }
        btnArt.setOnClickListener { filterCategory("예체능") }
        btnOneday.setOnClickListener { filterCategory("원데이클래스") }
    }

    // JSON 파일을 파싱하여 Academy 객체 리스트로 변환하는 함수
    private fun loadAcademiesFromJson(): List<Academy> {
        val list = mutableListOf<Academy>()
        try {
            val inputStream = assets.open("academies.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Academy(
                        name = obj.getString("name"),
                        category = obj.getString("category"),
                        address = obj.getString("address"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        tuition = obj.getInt("tuition"),
                        rating = obj.getDouble("rating"),
                        curriculum = obj.getString("curriculum"),
                        price = obj.getString("가격"),
                        image = obj.getString("이미지")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // 카테고리별로 학원들을 필터링해 주는 함수
    private fun filterCategory(category: String) {
        if (category == "전체") {
            academyAdapter.updateList(fullAcademyList)
        } else {
            val filteredList = fullAcademyList.filter { it.category == category }
            academyAdapter.updateList(filteredList)
        }
    }
}