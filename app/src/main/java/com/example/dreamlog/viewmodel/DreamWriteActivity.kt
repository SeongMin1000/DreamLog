package com.example.dreamlog.viewmodel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.dreamlog.R
import com.example.dreamlog.util.camera.CameraHelper
import com.example.dreamlog.util.EmotionAnalyzer
import com.example.dreamlog.util.camera.CameraHelper.rotateBitmapIfRequired
import java.io.File
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.api.preprocessDreamText
import com.example.dreamlog.api.ImageGenRetrofitInstance
import com.example.dreamlog.databinding.ActivityDreamWriteBinding
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Message
import com.example.dreamlog.model.ChatResponse
import com.example.dreamlog.model.ImageGenerationRequest
import com.example.dreamlog.model.ImageGenerationResponse
import com.google.android.material.navigation.NavigationView
import com.bumptech.glide.Glide
import com.example.dreamlog.util.DreamWorker
import kotlinx.coroutines.tasks.await
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.launch


class DreamWriteActivity : BaseActivity(){

    private lateinit var binding:ActivityDreamWriteBinding


    private lateinit var drawerToggle: ActionBarDrawerToggle  // 👈 추가

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var photoFile: File? = null
    private lateinit var imageBitmap: Bitmap

    private var emotionResult: String = ""
    private var gptInterpretation: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDreamWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ 이 시점에 뷰가 inflate 되었기 때문에 이제 사용 가능
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = binding.drawerLayout


        // 툴바 및 네비바 불러오기
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        binding.btnGenerateInterpretation.setOnClickListener {
            val rawUserDream = binding.editDream.text.toString()
            if (rawUserDream.isBlank()) {
                Toast.makeText(this, "먼저 꿈을 입력하세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userDream = preprocessDreamText(rawUserDream)
            binding.textGptResult.text = "GPT 해석 중..."

            val request = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "너는 꿈 해석 전문가야. 사용자가 말한 꿈을 심리학적으로 분석해서 의미를 설명해줘. 100자 이내로."),
                    Message(role = "user", content = userDream)
                )
            )

            // 🔄 코루틴 기반으로 GPT 호출
            lifecycleScope.launch {
                try {
                    val response = GptRetrofitInstance.api.getChatCompletion(request)
                    val reply = response.choices.firstOrNull()?.message?.content
                    gptInterpretation = reply ?: ""
                    binding.textGptResult.text = reply ?: "GPT의 응답이 비어있습니다."
                } catch (e: Exception) {
                    binding.textGptResult.text = "GPT 요청 실패: ${e.localizedMessage}"
                }
            }
        }




        // 📸 카메라 실행 후 결과 처리
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            CameraHelper.previewImage(binding.imagePreview)
        }

        // 📸 권한 요청 결과 처리
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val path = CameraHelper.getCurrentPhotoPath()
            if (path != null) {
                val rawBitmap = BitmapFactory.decodeFile(path)
                val correctedBitmap = rotateBitmapIfRequired(path, rawBitmap) // ⭐ 회전 보정
                imageBitmap = correctedBitmap
                binding.imagePreview.setImageBitmap(correctedBitmap)
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
        // 📸 카메라 버튼 클릭
        binding.btnOpenCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                photoFile = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        // 모델 초기화 (onCreate에서 딱 한 번)
        EmotionAnalyzer.initModel(this)

        // 사진을 찍은 뒤 imageBitmap에 저장해놨다고 가정하고,
        // 감정분석 버튼 클릭 시 분석 수행
        binding.btnAnalyzeEmotion.setOnClickListener {
            val emotion = EmotionAnalyzer.analyze(imageBitmap)
            emotionResult = emotion // ⭐ 저장
            binding.textEmotionResult.text = "감정 분석 결과: $emotion"

        }


        // 이미지 생성 버튼 클릭 시
        binding.btnGenerateImage.setOnClickListener {
            val uid = intent.getStringExtra("uid") ?: return@setOnClickListener
            val dreamText = binding.editDream.text.toString().trim()

            if (dreamText.isBlank() || emotionResult.isBlank() || gptInterpretation.isBlank()) {
                Toast.makeText(this, "꿈, 감정, GPT 해석을 모두 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 백그라운드 작업 요청 (GPT 프롬프트 + 이미지 생성 + 저장까지)
            val workRequest = OneTimeWorkRequestBuilder<DreamWorker>()
                .setInputData(
                    workDataOf(
                        "uid" to uid,
                        "dreamText" to dreamText,
                        "emotion" to emotionResult,
                        "gptInterpretation" to gptInterpretation
                    )
                )
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)

            // 사용자 입장에서 빠르게 전환
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }


    }
}
