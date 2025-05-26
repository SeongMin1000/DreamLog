package com.example.dreamlog.viewmodel

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ActivityResultBinding
import com.example.dreamlog.util.EmotionAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.api.preprocessDreamText
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Message
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.dreamlog.R
import com.example.dreamlog.util.camera.CameraHelper
import com.google.android.material.navigation.NavigationView

class ResultActivity : BaseActivity() {
    private lateinit var binding: ActivityResultBinding
    private var dreamText: String = ""
    private var photoPath: String? = null
    private var emotionResult: String = ""
    private var gptResult: String = ""
    private var docId: String? = null

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

        // 1. 데이터 받기
        dreamText = intent.getStringExtra("dreamText") ?: ""
        photoPath = intent.getStringExtra("photoPath")
        val uid = intent.getStringExtra("uid")
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ""

        // 2. 꿈과 사진 표시
        binding.textDream.text = dreamText
        photoPath?.let { path ->
            val rawBitmap = BitmapFactory.decodeFile(path)
            val correctedBitmap = CameraHelper.rotateBitmapIfRequired(path, rawBitmap)
            binding.imagePreview.setImageBitmap(correctedBitmap)
        }


        // 3. 감정 분석
        EmotionAnalyzer.initModel(this)
        binding.btnAnalyzeEmotion.setOnClickListener {
            if (photoPath == null) {
                Toast.makeText(this, "이미지가 없습니다!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bitmap = BitmapFactory.decodeFile(photoPath)
            val emotion = EmotionAnalyzer.analyze(bitmap)
            emotionResult = emotion
            binding.textEmotionResult.text = "감정 분석 결과: $emotion"
        }

        // 4. GPT 해석
        binding.btnGenerateInterpretation.setOnClickListener {
            if (dreamText.isBlank()) {
                Toast.makeText(this, "꿈을 먼저 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.textGptResult.text = "GPT 해석 중..."
            val userDream = preprocessDreamText(dreamText)
            val request = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "너는 꿈 해석 전문가야. 사용자가 말한 꿈을 심리학적으로 분석해서 의미를 설명해줘. 100자 이내로."),
                    Message(role = "user", content = userDream)
                )
            )
            lifecycleScope.launch {
                try {
                    val response = GptRetrofitInstance.api.getChatCompletion(request)
                    val reply = response.choices.firstOrNull()?.message?.content
                    gptResult = reply ?: ""
                    binding.textGptResult.text = reply ?: "GPT 응답이 비어있습니다."
                } catch (e: Exception) {
                    binding.textGptResult.text = "GPT 요청 실패: ${e.localizedMessage}"
                }
            }
        }

        // 5. 이미지 생성/DB저장 및 실시간 리스너 등록
        binding.btnGenerateImage.setOnClickListener {
            if (emotionResult.isBlank() || gptResult.isBlank()) {
                Toast.makeText(this, "감정분석과 GPT 해석을 모두 진행해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1) docId 미리 할당
            val docRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("dreams").document() // 새 문서 미리 할당
            docId = docRef.id

            // 2) WorkManager로 이미지 생성 + 저장 요청
            val data = workDataOf(
                "uid" to uid,
                "dreamText" to dreamText,
                "emotion" to emotionResult,
                "gptInterpretation" to gptResult,
                "docId" to docId!!
            )
            val workRequest = OneTimeWorkRequestBuilder<com.example.dreamlog.util.DreamWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(this).enqueue(workRequest)

            Toast.makeText(this, "꿈이 저장되고 이미지 생성이 시작됩니다.", Toast.LENGTH_SHORT).show()

            // 3) 실시간 리스너로 imageUrl 변경 감지 → 이미지뷰 표시
            listenDreamImageUrl(uid, docId!!)
        }
    }

    // 실시간 리스너로 imageUrl 업데이트 감지
    private fun listenDreamImageUrl(uid: String, docId: String) {
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("dreams").document(docId)
            .addSnapshotListener { docSnap, error ->
                if (error != null || docSnap == null) return@addSnapshotListener
                val imageUrl = docSnap.getString("imageUrl")
                Log.d("ResultActivity", "imageUrl: $imageUrl") // <- 반드시 추가!
                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .into(binding.generatedImageView)
                    binding.textImagePrompt.text = "이미지 생성 완료!"
                }
            }
    }
}
