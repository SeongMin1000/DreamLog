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
import com.example.dreamlog.databinding.ActivityDreamWriteBinding
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Message
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class DreamWriteActivity : BaseActivity() {

    private lateinit var binding: ActivityDreamWriteBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                photoFile = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val path = CameraHelper.getCurrentPhotoPath()
            if (path != null) {
                val rawBitmap = BitmapFactory.decodeFile(path)
                val correctedBitmap = rotateBitmapIfRequired(path, rawBitmap)
                // 분석용 64x64 저장
                imageBitmap = Bitmap.createScaledBitmap(correctedBitmap, 64, 64, true)
                // 미리보기는 큼직하게(200x200)
                val previewBitmap = Bitmap.createScaledBitmap(correctedBitmap, 200, 200, true)
                binding.imagePreview.setImageBitmap(previewBitmap)
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnOpenCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                photoFile = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

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

        EmotionAnalyzer.initModel(this)

        // 감정분석 버튼 안전 처리
        binding.btnAnalyzeEmotion.setOnClickListener {
            if (!::imageBitmap.isInitialized) {
                Toast.makeText(this, "먼저 사진을 찍어주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val emotion = EmotionAnalyzer.analyze(imageBitmap)
            emotionResult = emotion
            binding.textEmotionResult.text = "감정 분석 결과: $emotion"
        }

        binding.btnGenerateImage.setOnClickListener {
            val uid = intent.getStringExtra("uid") ?: return@setOnClickListener
            val dreamText = binding.editDream.text.toString().trim()
            if (dreamText.isBlank() || emotionResult.isBlank() || gptInterpretation.isBlank()) {
                Toast.makeText(this, "꿈, 감정, GPT 해석을 모두 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val workRequest = OneTimeWorkRequestBuilder<com.example.dreamlog.util.DreamWorker>()
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
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
