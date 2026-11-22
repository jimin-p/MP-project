package com.example.mp2026.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mp2026.Academy
import com.google.firebase.firestore.ListenerRegistration

data class AcademyUiState(
    val items: List<Academy> = emptyList(), val search: AcademySearch = AcademySearch(),
    val loading: Boolean = true, val fromCache: Boolean = false, val error: String? = null
)

class AcademyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AcademyRepository()
    private val preferences = SearchPreferences(application)
    private var allAcademies = emptyList<Academy>()
    private var registration: ListenerRegistration? = null
    private val mutableState = MutableLiveData(AcademyUiState(search = preferences.load()))
    val state: LiveData<AcademyUiState> = mutableState
    val search: AcademySearch get() = mutableState.value!!.search
    fun start() {
        if (registration != null) return
        updateSearch(preferences.load())
        mutableState.value = mutableState.value!!.copy(loading = true, error = null)
        registration = repository.observe { result ->
            result.onSuccess { snapshot ->
                allAcademies = snapshot.academies
                mutableState.value = AcademyUiState(search.applyTo(allAcademies), search,
                    loading = false, fromCache = snapshot.fromCache)
            }.onFailure { error ->
                Log.e("AcademyBackend", "Academy query failed", error)
                allAcademies = emptyList()
                mutableState.value = mutableState.value!!.copy(items = emptyList(), loading = false,
                    error = BackendErrors.message(error))
            }
        }
    }
    fun updateSearch(value: AcademySearch) {
        preferences.save(value)
        mutableState.value = mutableState.value!!.copy(search = value, items = value.applyTo(allAcademies))
    }
    fun retry() { stop(); start() }
    fun stop() { registration?.remove(); registration = null }
    override fun onCleared() { stop() }
}
