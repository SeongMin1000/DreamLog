package com.example.dreamlog.viewmodel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.dreamlog.databinding.ItemDreamBinding
import com.example.dreamlog.util.camera.CameraHelper
import com.example.dreamlog.util.EmotionAnalyzer
import com.example.dreamlog.util.camera.CameraHelper.rotateBitmapIfRequired
import java.io.File
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Message
import com.example.dreamlog.model.ChatResponse


class DreamWriteActivity : AppCompatActivity() {

    private lateinit var binding: ItemDreamBinding

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var photoFile: File? = null
    private lateinit var imageBitmap: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ItemDreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnGenerateInterpretation.setOnClickListener {
            val userDream = binding.editDream.text.toString()

            if (userDream.isBlank()) {
                Toast.makeText(this, "먼저 꿈을 입력하세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.textGptResult.text = "GPT 해석 중..."

            val request = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "너는 꿈 해석 전문가야. 사용자가 말한 꿈을 심리학적으로 분석해서 의미를 설명해줘."),
                    Message(role = "user", content = userDream)
                )
            )

            GptRetrofitInstance.api.getChatCompletion(request).enqueue(object : retrofit2.Callback<ChatResponse> {
                override fun onResponse(call: retrofit2.Call<ChatResponse>, response: retrofit2.Response<ChatResponse>) {
                    if (response.isSuccessful) {
                        val gptReply = response.body()?.choices?.firstOrNull()?.message?.content
                        binding.textGptResult.text = gptReply ?: "GPT의 응답이 비어있습니다."
                    } else {
                        binding.textGptResult.text = "GPT 응답 실패: ${response.code()}"
                    }
                }

                override fun onFailure(call: retrofit2.Call<ChatResponse>, t: Throwable) {
                    binding.textGptResult.text = "GPT 요청 실패: ${t.localizedMessage}"
                }
            })
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
            binding.textEmotionResult.text = "감정 분석 결과: $emotion"
        }
    }
}
