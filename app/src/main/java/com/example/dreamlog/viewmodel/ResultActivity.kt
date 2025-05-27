package com.example.dreamlog.viewmodel

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ActivityResultBinding
import com.example.dreamlog.R
import com.google.android.material.navigation.NavigationView



class ResultActivity : BaseActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 및 네비게이션 설정
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        // 1. Intent에서 데이터 받아오기
        // val dreamText = intent.getStringExtra("dreamText") ?: ""
        val emotion = intent.getStringExtra("emotion") ?: ""
        val gptResult = intent.getStringExtra("gptResult") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        // 2. UI에 값 표시
        //binding.textDream.text = dreamText
        binding.textEmotionResult.text = "감정 분석 결과: $emotion"

        // GPT 해석 결과 표시!
        binding.textGptResult.text = if (gptResult.isNotBlank()) gptResult else "GPT 해석 결과가 없습니다."

        // 3. 생성 이미지 표시
        if (imageUrl.isNotBlank()) {
//            binding.textImagePrompt.text = "이미지 생성 완료!"
            Glide.with(this)
                .load(imageUrl)
                .into(binding.generatedImageView)
        } else {
           // binding.textImagePrompt.text = "이미지가 없습니다."
            binding.generatedImageView.setImageDrawable(null)
        }
    }
}

