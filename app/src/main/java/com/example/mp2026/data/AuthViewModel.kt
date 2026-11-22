package com.example.mp2026.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class AuthUiState(val busy: Boolean = false, val signedIn: Boolean = false, val message: String? = null)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val mutableState = MutableLiveData(AuthUiState())
    val state: LiveData<AuthUiState> = mutableState
    fun authenticate(email: String, password: String, create: Boolean) {
        if (mutableState.value!!.busy) return
        mutableState.value = AuthUiState(busy = true)
        val task = if (create) repository.signUp(email, password) else repository.signIn(email, password)
        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                mutableState.value = AuthUiState(signedIn = true)
            } else {
                val error = result.exception ?: IllegalStateException("Authentication failed")
                Log.w("AuthBackend", "Authentication failed", error)
                mutableState.value = AuthUiState(message = BackendErrors.message(error))
            }
        }
    }
    fun resetPassword(email: String) {
        if (mutableState.value!!.busy) return
        mutableState.value = AuthUiState(busy = true)
        repository.resetPassword(email).addOnCompleteListener { task ->
            mutableState.value = AuthUiState(message = if (task.isSuccessful)
                "가입된 이메일이라면 비밀번호 재설정 메일을 확인해 주세요."
            else BackendErrors.message(task.exception ?: IllegalStateException()))
        }
    }
    fun consumeMessage() { mutableState.value = mutableState.value!!.copy(message = null) }
}
