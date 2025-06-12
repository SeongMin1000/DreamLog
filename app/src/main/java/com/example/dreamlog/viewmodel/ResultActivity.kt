package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ActivityResultBinding
import com.example.dreamlog.R
import com.example.dreamlog.model.Dream
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        // Intent 데이터 받기
        val dreamText = intent.getStringExtra("dreamText") ?: ""
        val emotion = intent.getStringExtra("emotion") ?: ""
        val gptResult = intent.getStringExtra("gptResult") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val fromMain = intent.getBooleanExtra("fromMain", false)
        if (fromMain) {
            binding.btnSave.isEnabled = false
            binding.btnSave.alpha = 0.5f // 흐리게 표시 (선택)
            binding.btnSave.text = "저장됨" // 버튼 텍스트 변경
        }

        // UI 표시
        binding.textEmotionResult.text = "$emotion"
        binding.textGptResult.text = if (gptResult.isNotBlank()) gptResult else "GPT 해석 결과가 없습니다."
        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.generatedImageView)
        } else {
            binding.generatedImageView.setImageDrawable(null)
        }

        // 저장 버튼 클릭 리스너 (Firestore 저장)
        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false // 중복 저장 방지
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(uid)
                .collection("dreams").document()
            val docId = docRef.id
            val dream = Dream(
                        dreamText = dreamText,
                        emotion = emotion,
                        gptInterpretation = gptResult,
                        imageUrl = imageUrl,
                        timestamp = com.google.firebase.Timestamp.now()
                    )
            docRef.set(dream)
                .addOnSuccessListener {
                    Toast.makeText(this, "꿈이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    // MainActivity로 이동하고 ResultActivity는 종료
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // 메인으로 이동 시 스택 정리
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true // 실패하면 버튼 다시 활성화
                }
        }

        // 캘린더 등록 버튼 클릭 리스너
        binding.btnAddToCalendar.setOnClickListener {
            try {
                val calendarIntent = Intent(Intent.ACTION_INSERT)
                calendarIntent.data = CalendarContract.Events.CONTENT_URI
                calendarIntent.putExtra(CalendarContract.Events.TITLE, "꿈 기록 - $emotion")
                calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, gptResult)
                calendarIntent.putExtra(
                    CalendarContract.Events.EVENT_LOCATION,
                    "Dreamlog 앱"
                )
                // 바로 오늘 날짜, 일정 시간(1시간)으로 등록
                val startMillis = System.currentTimeMillis()
                val endMillis = startMillis + 60 * 60 * 1000
                calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                startActivity(calendarIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "캘린더에 등록할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
