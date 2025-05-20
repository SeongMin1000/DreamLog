package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.dreamlog.R
import com.example.dreamlog.model.User

// firebaseDB 사용
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEdit = findViewById<EditText>(R.id.editEmail)
        val passwordEdit = findViewById<EditText>(R.id.editPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val signupBtn = findViewById<Button>(R.id.btnSignup)
        val errorText = findViewById<TextView>(R.id.textError)

        loginBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = "이메일과 비밀번호를 모두 입력하세요."
                return@setOnClickListener
            }

            signIn(email, password, errorText)
        }

        signupBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                errorText.text = "이메일과 비밀번호를 모두 입력하세요."
                return@setOnClickListener
            }

            signUp(email, password, errorText)
        }
    }

    // email & password Authentication 방식 사용
    // 로그인
    private fun signIn(email: String, password: String, errorText: TextView) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    errorText.text = "로그인 실패: ${task.exception?.message}"
                }
            }
    }

    // 회원가입
    private fun signUp(email: String, password: String, errorText: TextView) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: return@addOnCompleteListener

                    val userData = User(
                        userEmail = email,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            errorText.text = "사용자 정보 저장 실패: ${it.message}"
                        }
                } else {
                    errorText.text = "회원가입 실패: ${task.exception?.message}"
                }
            }
    }
}
