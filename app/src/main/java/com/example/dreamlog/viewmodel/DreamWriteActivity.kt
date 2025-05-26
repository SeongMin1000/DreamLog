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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.dreamlog.R
import com.example.dreamlog.databinding.ActivityDreamWriteBinding
import com.example.dreamlog.util.camera.CameraHelper
import com.google.android.material.navigation.NavigationView
import java.io.File

class DreamWriteActivity : BaseActivity() {

    private lateinit var binding: ActivityDreamWriteBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var photoFile: File? = null
    private var imageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDreamWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                photoFile = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 카메라 실행 런처
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val path = CameraHelper.getCurrentPhotoPath()
            if (path != null) {
                val rawBitmap = BitmapFactory.decodeFile(path)
                val correctedBitmap = CameraHelper.rotateBitmapIfRequired(path, rawBitmap)
                imageBitmap = correctedBitmap
                // 미리보기 용도 (적당한 크기로)
                val previewBitmap = Bitmap.createScaledBitmap(correctedBitmap, 200, 200, true)
                binding.imagePreview.setImageBitmap(previewBitmap)
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 셀카 찍기 버튼
        binding.btnOpenCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                photoFile = CameraHelper.dispatchTakePictureIntent(this, cameraLauncher)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // "다음" 버튼 (ResultActivity로 이동)
        binding.btnNext.setOnClickListener {
            val dreamText = binding.editDream.text.toString().trim()
            if (dreamText.isBlank() || photoFile == null) {
                Toast.makeText(this, "꿈을 입력하고 사진을 찍어주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // ResultActivity로 꿈 텍스트와 이미지 경로 전달
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("dreamText", dreamText)
                putExtra("photoPath", photoFile!!.absolutePath)
            }
            startActivity(intent)
            finish()
        }
    }
}