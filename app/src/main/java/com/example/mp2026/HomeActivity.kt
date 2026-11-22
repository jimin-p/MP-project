package com.example.mp2026

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.mp2026.data.*
import com.example.mp2026.ui.AuthenticatedActivity
import java.util.Locale

class HomeActivity : AuthenticatedActivity() {
    private lateinit var viewModel: AcademyViewModel
    private lateinit var location: CurrentLocation
    private var selected: Academy? = null
    private val categories = mapOf(R.id.btnHomeAll to "전체", R.id.btnHomeEdu to "입시교육",
        R.id.btnHomeLang to "어학", R.id.btnHomeCert to "자격증", R.id.btnHomeArt to "예체능",
        R.id.btnHomeOneday to "원데이클래스")
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) requestLocation()
        else toast("위치 권한 없이도 이름·카테고리로 검색할 수 있습니다.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        viewModel = ViewModelProvider(this)[AcademyViewModel::class.java]
        location = CurrentLocation(this)
        findViewById<View>(R.id.searchBarLayout).setOnClickListener { searchDialog() }
        findViewById<View>(R.id.btnClearSearch).setOnClickListener {
            viewModel.updateSearch(viewModel.search.copy(keyword = ""))
        }
        categories.forEach { (id, category) ->
            findViewById<Button>(id).setOnClickListener { viewModel.updateSearch(viewModel.search.copy(category = category)) }
        }
        findViewById<Button>(R.id.btnShowList).setOnClickListener {
            startActivity(Intent(this, AcademyListActivity::class.java))
        }
        findViewById<Button>(R.id.btnSearchNearby).setOnClickListener {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        findViewById<Button>(R.id.btnHomeFilter).setOnClickListener { filterDialog() }
        findViewById<TextView>(R.id.tvHomeSort).setOnClickListener { sortDialog() }
        findViewById<TextView>(R.id.tvHomeResultTitle).setOnClickListener { if (hasSession) viewModel.retry() }
        findViewById<Button>(R.id.btnDetail).setOnClickListener {
            selected?.let { academy ->
                // Temporary binding to the existing button; the frontend can replace this with a detail screen.
                AlertDialog.Builder(this).setTitle(academy.name)
                    .setMessage("${academy.category}\n${academy.address}\n\n${academy.curriculum}\n\n${academy.price}\n★ ${academy.rating}")
                    .setPositiveButton("확인", null).show()
            }
        }
        viewModel.state.observe(this, ::render)
        if (savedInstanceState == null) {
            val auth = AuthRepository()
            auth.currentUser?.let { user ->
                auth.syncProfile(user).addOnFailureListener(this) { error ->
                    Log.w("AuthBackend", "Profile sync failed; Authentication session remains valid", error)
                    toast("로그인은 완료됐지만 프로필을 저장하지 못했습니다. 다음 홈 진입 시 다시 시도합니다.")
                }
            }
        }
    }

    override fun onStart() { super.onStart(); if (hasSession) viewModel.start() }
    override fun onStop() { location.cancel(); viewModel.stop(); super.onStop() }

    private fun render(state: AcademyUiState) {
        selected = state.items.firstOrNull()
        findViewById<TextView>(R.id.tvSearchQuery).text = state.search.keyword.ifBlank { "학원명·지역·관심 분야 검색" }
        findViewById<TextView>(R.id.tvHomeResultTitle).text = when {
            state.error != null -> "조회 실패 · 눌러서 재시도"
            state.loading -> "학원을 불러오는 중…"
            else -> "검색 결과 ${state.items.size}곳" + if (state.fromCache) " · 캐시" else ""
        }
        findViewById<TextView>(R.id.tvHomeSort).text = when (state.search.sort) {
            AcademySort.NAME -> "이름순 ▾"
            AcademySort.RATING -> "평점순 ▾"
            AcademySort.REVIEW_COUNT -> "후기순 ▾"
            AcademySort.DISTANCE -> "거리순 ▾"
            AcademySort.TUITION -> "수강료순 ▾"
        }
        categories.forEach { (id, category) ->
            findViewById<Button>(id).apply {
                isSelected = state.search.category == category
                alpha = if (isSelected) 1f else 0.65f
            }
        }
        findViewById<TextView>(R.id.tvHomeName).text = selected?.name ?: when {
            state.error != null -> state.error
            state.loading -> "잠시 기다려 주세요."
            state.fromCache -> "저장된 학원이 없습니다. 서버 연결을 확인해 주세요."
            else -> "검색 조건에 맞는 학원이 없습니다."
        }
        findViewById<TextView>(R.id.tvHomeCategory).text = selected?.let { "${it.category} · ${it.curriculum}" }.orEmpty()
        findViewById<TextView>(R.id.tvHomeRating).text = selected?.let { academy ->
            val distance = state.search.origin?.let { origin ->
                " · 기준 위치에서 " + String.format(Locale.KOREA, "%.1fkm", AcademySearch.distanceKm(origin, academy))
            }.orEmpty()
            "★ ${academy.rating} (${academy.reviewCount})$distance"
        }.orEmpty()
        findViewById<TextView>(R.id.tvHomePrice).text = selected?.price.orEmpty()
        findViewById<Button>(R.id.btnDetail).isEnabled = selected != null
    }

    private fun searchDialog() {
        val field = EditText(this).apply {
            hint = "학원명, 지역 또는 관심 분야"
            setSingleLine(true)
            setText(viewModel.search.keyword)
        }
        AlertDialog.Builder(this).setTitle("학원 검색").setView(field)
            .setNegativeButton("취소", null)
            .setPositiveButton("검색") { _, _ -> viewModel.updateSearch(viewModel.search.copy(keyword = field.text.toString().trim())) }
            .show()
    }

    private fun filterDialog() {
        val search = viewModel.search
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }
        fun input(hintText: String, value: String): EditText = EditText(this).apply {
            hint = hintText
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(value)
            fields.addView(this)
        }
        val tuition = input("최대 월 수강료 (원, 비우면 제한 없음)", search.maxTuition?.toString().orEmpty())
        val rating = input("최소 평점 (0~5)", search.minRating.toString())
        val radius = input("검색 반경 (km, 비우면 제한 없음)", search.radiusKm?.toString().orEmpty())
        val dialog = AlertDialog.Builder(this).setTitle("검색 필터").setView(fields)
            .setNegativeButton("취소", null)
            .setNeutralButton("초기화") { _, _ -> viewModel.updateSearch(AcademySearch(origin = search.origin)) }
            .setPositiveButton("적용", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val max = tuition.text.toString().trim().let { if (it.isEmpty()) null else it.toIntOrNull() }
                val min = rating.text.toString().trim().ifEmpty { "0" }.toDoubleOrNull()
                val km = radius.text.toString().trim().let { if (it.isEmpty()) null else it.toDoubleOrNull() }
                when {
                    tuition.text.isNotBlank() && (max == null || max < 0) -> tuition.error = "0 이상의 정수를 입력해 주세요."
                    min == null || !min.isFinite() || min !in 0.0..5.0 -> rating.error = "0~5 사이의 평점을 입력해 주세요."
                    radius.text.isNotBlank() && (km == null || !km.isFinite() || km <= 0) -> radius.error = "0보다 큰 거리를 입력해 주세요."
                    km != null && search.origin == null -> radius.error = "홈의 위치 검색 버튼으로 기준 위치를 먼저 설정해 주세요."
                    else -> {
                        viewModel.updateSearch(search.copy(maxTuition = max, minRating = min!!, radiusKm = km))
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun sortDialog() {
        val sorts = arrayOf(AcademySort.NAME, AcademySort.RATING, AcademySort.REVIEW_COUNT, AcademySort.DISTANCE, AcademySort.TUITION)
        AlertDialog.Builder(this).setTitle("정렬")
            .setSingleChoiceItems(arrayOf("이름순", "평점순", "후기순", "거리순", "수강료 낮은순"), sorts.indexOf(viewModel.search.sort)) { dialog, index ->
                if (sorts[index] == AcademySort.DISTANCE && viewModel.search.origin == null) {
                    toast("홈의 위치 검색 버튼으로 기준 위치를 먼저 설정해 주세요.")
                } else {
                    viewModel.updateSearch(viewModel.search.copy(sort = sorts[index]))
                }
                dialog.dismiss()
            }.setNegativeButton("취소", null).show()
    }

    private fun requestLocation() {
        toast("현재 위치를 확인하고 있습니다.")
        location.request { point ->
            if (point == null) toast("현재 위치를 찾지 못했습니다. 위치 설정을 확인하고 다시 시도해 주세요.")
            else viewModel.updateSearch(viewModel.search.copy(origin = point, sort = AcademySort.DISTANCE))
        }
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
