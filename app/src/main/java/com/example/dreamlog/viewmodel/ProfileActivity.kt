package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.R
import com.example.dreamlog.databinding.ActivityProfileBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 및 네비게이션 설정
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        binding.btnBackToMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        loadUserProfile()
    }
    override fun onResume() {
        super.onResume()
        loadUserProfile()  // Firestore에서 최신 정보 다시 가져오는 함수
    }

    private fun loadUserProfile() {
        if (uid == null) return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.textName.text = doc.getString("name") ?: "User"
                binding.textEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: "unknown@email.com"
                binding.textComment.text = doc.getString("comment") ?: "한줄 소개가 없습니다."

                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).into(binding.imageProfile)
                } else {
                    binding.imageProfile.setImageResource(R.drawable.ic_profile_default)
                }
            }
    }
}
