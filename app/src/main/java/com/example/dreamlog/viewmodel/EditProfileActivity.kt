package com.example.dreamlog.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dreamlog.databinding.ActivityEditProfileBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val storageRef = FirebaseStorage.getInstance().reference
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🐛 Firebase Storage 연결 확인 로그

        Log.d("FirebaseDebug", FirebaseApp.getInstance().options.storageBucket ?: "No bucket")


        val toolbar = findViewById<Toolbar>(com.example.dreamlog.R.id.toolbar)
        val navigationView = findViewById<NavigationView>(com.example.dreamlog.R.id.navigationView)
        val drawerLayout = binding.drawerLayout
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        loadCurrentProfile()
        setupImagePicker()

        binding.imageProfileEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
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
                uploadImageToFirebase(selectedImageUri!!) { imageUrl ->
                    saveProfileData(name, comment, imageUrl)
                }
            } else {
                saveProfileData(name, comment, null)
            }
        }
    }


    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data!!.data
                if (uri != null) {
                    selectedImageUri = uri
                    Glide.with(this).load(uri).into(binding.imageProfileEdit)
                } else {
                    Toast.makeText(this, "이미지를 가져오는 데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
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
                Toast.makeText(this, "이미지 업로드 또는 URL 요청 실패: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseStorageFail", "이미지 업로드 또는 URL 요청 실패: ${it.localizedMessage}")
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
}
