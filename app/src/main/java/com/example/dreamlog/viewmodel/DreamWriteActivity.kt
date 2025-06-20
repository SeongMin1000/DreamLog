package com.example.dreamlog.viewmodel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.dreamlog.R
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.api.ImageGenRetrofitInstance
import com.example.dreamlog.api.preprocessDreamText
import com.example.dreamlog.databinding.ActivityDreamWriteBinding
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.ImageGenerationRequest
import com.example.dreamlog.model.Message
import com.example.dreamlog.util.EmotionAnalyzer
import com.example.dreamlog.util.camera.CameraHelper
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class DreamWriteActivity : BaseActivity() {

    private lateinit var binding: ActivityDreamWriteBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var photoPath: String? = null
    private var imageBitmap: android.graphics.Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDreamWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 감정 분석 모델 초기화
        EmotionAnalyzer.initModel(this)

        // 툴바 및 네비게이션 설정
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        // 카메라 권한 요청 런처
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                photoPath = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)?.absolutePath
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 카메라 실행 런처
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            photoPath = CameraHelper.getCurrentPhotoPath()
            // 카메라 실행 후 사진 촬영하지 않고 뒤로가기 시 에러 처리
            if (result.resultCode == RESULT_OK && photoPath != null) {
                val rawBitmap = BitmapFactory.decodeFile(photoPath!!)
                val correctedBitmap = CameraHelper.rotateBitmapIfRequired(photoPath!!, rawBitmap)
                binding.imagePreview.setImageBitmap(correctedBitmap)
                binding.imagePreview.setImageTintList(null)
                binding.imagePreview.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                binding.imagePreview.setPadding(0, 0, 0, 0)
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                val cameraDrawable = ContextCompat.getDrawable(this, R.drawable.ic_camera)
                binding.imagePreview.setImageDrawable(cameraDrawable)
                binding.imagePreview.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_gray_hint_text)))
                // XML의 원래 설정으로 복원 (fitCenter, 90dp padding)
                binding.imagePreview.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                val paddingPx = (90 * resources.displayMetrics.density).toInt()
                binding.imagePreview.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.imagePreview.invalidate()  // 뷰 강제 갱신
                photoPath = null
            }
        }

        // 셀카 찍기
        binding.imagePreview.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                photoPath = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)?.absolutePath
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }


        // 결과 보기(다음) 버튼
        binding.btnNext.setOnClickListener {
            val dreamText = binding.editDream.text.toString().trim()

            if (dreamText.isBlank()) {
                Toast.makeText(this, "꿈 내용을 입력하세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (photoPath.isNullOrBlank()) {
                Toast.makeText(this, "셀카를 먼저 찍어주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            Toast.makeText(this, "생성 중...", Toast.LENGTH_SHORT).show()

            // 로딩 표시 + 전체 터치 차단
            binding.progressBar.visibility = android.view.View.VISIBLE
            binding.blockTouchView?.visibility = android.view.View.VISIBLE
            binding.btnNext.isEnabled = false

            lifecycleScope.launch {
                try {
                    // 1. 감정 분석
                    var emotionResult = ""
                    if (photoPath != null) {
                        val rawBitmap = BitmapFactory.decodeFile(photoPath!!)
                        emotionResult = EmotionAnalyzer.analyze(rawBitmap)
                    }

                    // 2. GPT 해석
                    var gptResult = ""
                    if (dreamText.isNotBlank()) {
                        val userDream = preprocessDreamText(dreamText)
                        val request = ChatRequest(
                            messages = listOf(
                                Message(role = "system", content = "너는 꿈 해석 전문가야. 사용자가 말한 꿈을 심리학적으로 분석해서 의미를 설명해줘. 100자 이내로."),
                                Message(role = "user", content = userDream)
                            )
                        )
                        val response = GptRetrofitInstance.api.getChatCompletion(request)
                        gptResult = response.choices.firstOrNull()?.message?.content ?: ""
                    }

                    // 3. 이미지 생성(GPT 프롬프트 → 이미지 생성 API)
                    val promptRequest = ChatRequest(
                        messages = listOf(
                            Message(
                                role = "system",
                                content = """
                                            너는 이미지 디자이너야.
                                            사용자의 꿈 해석과 감정에 어울리는 
                                            'YouTube Music Recap' 스타일 + '몽환적(dreamy, surreal, ethereal)' 느낌의 일러스트 프롬프트를 만들어줘.
                                            - 감정과 해석을 추상적으로 시각화
                                            - 팝아트적 요소와 몽환적인 분위기 강조
                                            영어 한 문장으로 묘사 위주로 작성
                                        """.trimIndent()
                            ),
                            Message(
                                role = "user",
                                content = "감정: $emotionResult\n해석: $gptResult\n이 내용을 이미지로 형상화할 수 있는 프롬프트를 영어로 작성해줘."
                            )
                        )
                    )
                    val promptResponse = GptRetrofitInstance.api.getChatCompletion(promptRequest)
                    val imagePrompt = promptResponse.choices.firstOrNull()?.message?.content ?: ""
                    val imageResponse = ImageGenRetrofitInstance.api.generateImage(
                        ImageGenerationRequest(prompt = imagePrompt)
                    )
                    val imageUrl = imageResponse.data.firstOrNull()?.url ?: ""

                    // 5. 모든 작업 완료 후 결과화면 이동
                    // 결과 화면에서 뒤로 가기 시 메인 화면으로 이동하는 현상 방지
                    if (!this@DreamWriteActivity.isFinishing && !this@DreamWriteActivity.isDestroyed) {
                        val intent = Intent(this@DreamWriteActivity, ResultActivity::class.java).apply {
                            putExtra("dreamText", dreamText)
                            putExtra("emotion", emotionResult)
                            putExtra("gptResult", gptResult)
                            putExtra("imageUrl", imageUrl)
                        }
                        startActivity(intent)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@DreamWriteActivity, "분석 또는 생성 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    // 로딩 해제 + 전체 터치 해제
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.blockTouchView?.visibility = android.view.View.GONE
                    binding.btnNext.isEnabled = true
                }
            }
        }
    }
}