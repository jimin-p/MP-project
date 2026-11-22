package com.example.mp2026

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mp2026.data.AuthViewModel

class SignUpActivity : AppCompatActivity() {
    private var navigating = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val model = ViewModelProvider(this)[AuthViewModel::class.java]
        val email = findViewById<EditText>(R.id.etSignUpEmail)
        email.imeHintLocales = android.os.LocaleList(java.util.Locale.ENGLISH)
        val password = findViewById<EditText>(R.id.etSignUpPassword)
        val confirm = findViewById<EditText>(R.id.etSignUpConfirm)
        val submit = findViewById<Button>(R.id.btnCreateAccount)
        val back = findViewById<Button>(R.id.btnBackToLogin)
        val error = findViewById<TextView>(R.id.tvSignUpError)
        back.setOnClickListener { finish() }
        submit.setOnClickListener {
            email.error = null; password.error = null; confirm.error = null
            when {
                !Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches() -> {
                    email.error = "올바른 이메일을 입력해 주세요."; email.requestFocus()
                }
                password.text.length < 6 -> {
                    password.error = "비밀번호를 6자 이상 입력해 주세요."; password.requestFocus()
                }
                password.text.toString() != confirm.text.toString() -> {
                    confirm.error = "비밀번호가 일치하지 않습니다."; confirm.requestFocus()
                }
                else -> model.authenticate(email.text.toString(), password.text.toString(), create = true)
            }
        }
        model.state.observe(this) { state ->
            submit.isEnabled = !state.busy
            back.isEnabled = !state.busy
            // Keep the editor/input connection alive while the network request runs.
            // The submit button and ViewModel already prevent duplicate requests.
            submit.text = if (state.busy) "가입 중…" else "가입하기"
            error.text = state.message.orEmpty()
            error.visibility = if (state.message == null) View.GONE else View.VISIBLE
            if (state.signedIn && !navigating) {
                navigating = true
                password.text.clear(); confirm.text.clear()
                Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                // Home synchronizes the Firebase Auth user's profile to users/{uid}.
                startActivity(Intent(this, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }
    }
}
