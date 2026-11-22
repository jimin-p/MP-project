package com.example.mp2026.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.mp2026.MainActivity
import com.google.firebase.auth.FirebaseAuth

abstract class AuthenticatedActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val authListener = FirebaseAuth.AuthStateListener { firebase ->
        if (firebase.currentUser == null) returnToLogin()
    }
    protected val hasSession: Boolean get() = auth.currentUser != null
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener)
        if (!hasSession) returnToLogin()
    }
    override fun onStop() { auth.removeAuthStateListener(authListener); super.onStop() }
    private fun returnToLogin() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
