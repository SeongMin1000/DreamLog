package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.dreamlog.R
import com.example.dreamlog.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editEmail = findViewById<EditText>(R.id.editSignupEmail)
        val editPassword = findViewById<EditText>(R.id.editSignupPassword)
        val editName = findViewById<EditText>(R.id.editSignupName)
        val editComment = findViewById<EditText>(R.id.editSignupComment)
        val editPhone = findViewById<EditText>(R.id.editSignupPhone)
        val btnSignup = findViewById<Button>(R.id.btnSignupSubmit)
        val textError = findViewById<TextView>(R.id.textSignupError)

        btnSignup.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val name = editName.text.toString().trim()
            val comment = editComment.text.toString().trim()
            val phone = editPhone.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                textError.text = "이메일, 비밀번호, 이름은 필수입니다."
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val user = User(
                            userEmail = email,
                            name = name,
                            comment = comment,
                            phone = phone,
                            profileImageUrl = "",
                            createdAt = System.currentTimeMillis()
                        )

                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                textError.text = "Firestore 저장 실패: ${it.message}"
                            }

                    } else {
                        textError.text = "회원가입 실패: ${task.exception?.message}"
                    }
                }
        }
    }
}
