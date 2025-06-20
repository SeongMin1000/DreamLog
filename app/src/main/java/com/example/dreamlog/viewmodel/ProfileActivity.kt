package com.example.dreamlog.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.R
import com.example.dreamlog.databinding.ActivityProfileBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

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

        // outlineProvider 설정 (clipToOutline 작동하게끔)
        binding.imageProfile.outlineProvider = ViewOutlineProvider.BACKGROUND
        setupImagePicker()

        binding.imageProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.textName.text.toString().trim()
            val phone = binding.editPhone.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            val comment = binding.editComment.text.toString().trim()

            if (uid == null) {
                Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadImageToFirebase(selectedImageUri!!) { imageUrl ->
                    saveProfileData(name, phone, comment, imageUrl)
                }
            } else {
                saveProfileData(name, phone, comment, null)
            }
        }

        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                if (uri != null) {
                    selectedImageUri = uri
                    // Glide 로드 실패 시 기본 이미지 대체
                    Glide.with(this)
                        .load(uri)
                        .error(R.drawable.ic_profile_default)
                        .into(binding.imageProfile)
                } else {
                    Toast.makeText(this, "이미지를 가져오는 데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserProfile() {
        if (uid == null) return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.textName.setText(doc.getString("name") ?: "")
                binding.editPhone.setText(doc.getString("phone") ?: "")
                binding.editEmail.setText(FirebaseAuth.getInstance().currentUser?.email ?: "")
                binding.editComment.setText(doc.getString("comment") ?: "")

                val imageUrl = doc.getString("profileImageUrl")

                if (selectedImageUri != null) {
                    // 사용자가 사진 선택했으면 그걸 우선 반영
                    Glide.with(this).load(selectedImageUri).into(binding.imageProfile)
                } else if (!imageUrl.isNullOrEmpty()) {
                    // 선택한 사진 없으면 서버에 있는 사진 반영
                    Glide.with(this).load(imageUrl).into(binding.imageProfile)
                } else {
                    // 아무것도 없으면 기본 이미지
                    binding.imageProfile.setImageResource(R.drawable.ic_profile_default)
                }

            }
    }

    private fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit) {
        val profileImageRef = storageRef.child("profileImages/${uid}_myprofile.jpg")
        val uploadTask = profileImageRef.putFile(uri)

        uploadTask
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("이미지 업로드 실패")
                }
                profileImageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                onSuccess(downloadUrl.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseStorageFail", "이미지 업로드 실패: ${it.localizedMessage}")
            }
    }

    private fun saveProfileData(name: String, phone: String, comment: String, imageUrl: String?) {
        if (uid == null) return

        val data = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "comment" to comment
        )
        if (imageUrl != null) {
            data["profileImageUrl"] = imageUrl
        }

        firestore.collection("users").document(uid).update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show()
                loadUserProfile() // 저장 직후 새로고침
            }
            .addOnFailureListener {
                Toast.makeText(this, "저장 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}
