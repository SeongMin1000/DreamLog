package com.example.dreamlog.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ActivityEditProfileBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val storageRef = FirebaseStorage.getInstance().reference
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 및 드로어 세팅
        val toolbar = findViewById<Toolbar>(com.example.dreamlog.R.id.toolbar)
        val navigationView = findViewById<NavigationView>(com.example.dreamlog.R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        loadCurrentProfile()

        // 프로필 이미지 클릭 시 갤러리 열기
        binding.imageProfileEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val comment = binding.editComment.text.toString().trim()

            if (uid == null) {
                Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                Log.d("EditProfile", "이미지 선택됨: $selectedImageUri")
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                if (inputStream == null) {
                    Toast.makeText(this, "이미지를 열 수 없습니다", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val profileImageRef = storageRef.child("profileImages/${uid}_${UUID.randomUUID()}.jpg")
                profileImageRef.putStream(inputStream)
                    .addOnSuccessListener {
                        Log.d("EditProfile", "이미지 업로드 성공")
                        profileImageRef.downloadUrl
                            .addOnSuccessListener { downloadUrl ->
                                Log.d("EditProfile", "다운로드 URL 획득 성공: $downloadUrl")
                                saveProfileData(name, comment, downloadUrl.toString())
                            }
                            .addOnFailureListener {
                                Log.e("EditProfile", "다운로드 URL 획득 실패", it)
                                Toast.makeText(this, "URL 요청 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Log.e("EditProfile", "이미지 업로드 실패", it)
                        Toast.makeText(this, "이미지 업로드 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.d("EditProfile", "이미지 선택되지 않음, 텍스트만 저장")
                saveProfileData(name, comment, null)
            }
        }

    }

    private fun loadCurrentProfile() {
        if (uid == null) return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.editName.setText(doc.getString("name") ?: "")
                binding.editComment.setText(doc.getString("comment") ?: "")
                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).into(binding.imageProfileEdit)
                }
            }
    }

    private fun saveProfileData(name: String, comment: String, imageUrl: String?) {
        if (uid == null) return

        val data = mutableMapOf<String, Any>(
            "name" to name,
            "comment" to comment
        )
        if (imageUrl != null) {
            data["profileImageUrl"] = imageUrl
        }

        firestore.collection("users").document(uid).update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.imageProfileEdit.setImageURI(selectedImageUri)
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 101
    }
}
