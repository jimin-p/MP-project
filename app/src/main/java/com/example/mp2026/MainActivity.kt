package com.example.mp2026

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mp2026.data.AuthRepository
import com.example.mp2026.data.AuthViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel
    private lateinit var email: EditText
    private lateinit var password: EditText
    private var navigating = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        email = findViewById(R.id.etEmail)
        email.imeHintLocales = android.os.LocaleList(java.util.Locale.ENGLISH)
        password = findViewById(R.id.etPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val signup = findViewById<TextView>(R.id.tvSignUp)
        val reset = findViewById<TextView>(R.id.tvFindPassword)
        login.setOnClickListener {
            if (validate()) viewModel.authenticate(email.text.toString(), password.text.toString(), false)
        }
        signup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        reset.setOnClickListener {
            if (validateEmail()) {
                val address = email.text.toString().trim()
                AlertDialog.Builder(this).setTitle("비밀번호 재설정")
                    .setMessage("$address 주소로 재설정 메일을 보낼까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("보내기") { _, _ -> viewModel.resetPassword(address) }.show()
            }
        }
        findViewById<TextView>(R.id.tvFindEmail).setOnClickListener {
            toast("가입할 때 사용한 이메일을 확인해 주세요. 이메일 찾기는 본인 확인 기능이 준비된 후 제공됩니다.")
        }
        findViewById<Button>(R.id.btnKakaoLogin).setOnClickListener { toast("카카오 로그인은 준비 중입니다. 이메일로 이용해 주세요.") }
        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener { toast("Google 로그인은 준비 중입니다. 이메일로 이용해 주세요.") }
        viewModel.state.observe(this) { state ->
            login.isEnabled = !state.busy
            signup.isEnabled = !state.busy
            reset.isEnabled = !state.busy
            login.text = if (state.busy) "처리 중…" else "로그인"
            state.message?.let { toast(it); viewModel.consumeMessage() }
            if (state.signedIn) openHome()
        }
    }
    override fun onStart() {
        super.onStart()
        if (AuthRepository().currentUser != null) openHome()
    }
    private fun validateEmail(): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()) {
            email.error = "올바른 이메일을 입력해 주세요."
            email.requestFocus()
            return false
        }
        return true
    }
    private fun validate(): Boolean {
        if (!validateEmail()) return false
        if (password.text.isEmpty()) {
            password.error = "비밀번호를 입력해 주세요."
            password.requestFocus()
            return false
        }
        return true
    }
    private fun openHome() {
        if (navigating) return
        navigating = true
        password.text.clear()
        startActivity(Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
