package com.example.mp2026

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mp2026.data.AcademyViewModel
import com.example.mp2026.ui.AuthenticatedActivity

class AcademyListActivity : AuthenticatedActivity() {
    private lateinit var viewModel: AcademyViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academy_list)
        viewModel = ViewModelProvider(this)[AcademyViewModel::class.java]
        val adapter = AcademyAdapter(emptyList())
        findViewById<RecyclerView>(R.id.rvAcademyList).apply {
            layoutManager = LinearLayoutManager(this@AcademyListActivity)
            this.adapter = adapter
        }
        val count = findViewById<TextView>(R.id.tvAcademyCount)
        count.setOnClickListener { if (hasSession) viewModel.retry() }
        val categories = mapOf(R.id.btnCatAll to "전체", R.id.btnCatEdu to "입시교육",
            R.id.btnCatLang to "어학", R.id.btnCatArt to "예체능", R.id.btnCatOneday to "원데이클래스")
        categories.forEach { (id, category) ->
            findViewById<Button>(id).setOnClickListener { viewModel.updateSearch(viewModel.search.copy(category = category)) }
        }
        viewModel.state.observe(this) { state ->
            adapter.updateList(state.items)
            count.text = when {
                state.error != null -> "${state.error}\n눌러서 다시 시도"
                state.loading -> "학원 정보를 불러오는 중…"
                else -> "우리 동네 학원 ${state.items.size}" + if (state.fromCache) " · 캐시" else ""
            }
            categories.forEach { (id, category) -> findViewById<Button>(id).isSelected = state.search.category == category }
        }
    }
    override fun onStart() { super.onStart(); if (hasSession) viewModel.start() }
    override fun onStop() { viewModel.stop(); super.onStop() }
}
