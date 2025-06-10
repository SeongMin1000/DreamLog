package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dreamlog.R
import com.example.dreamlog.adapter.DreamAdapter
import com.example.dreamlog.model.Dream
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

// firebaseDB 사용
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : BaseActivity() {
    private val dreamList = mutableListOf<Dream>()
    private lateinit var adapter: DreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val fabAddDream = findViewById<FloatingActionButton>(R.id.fabAddDream)

        // 툴바 및 네비바 불러오기
        setupToolbarAndDrawer(toolbar, drawerLayout, navigationView)
        setupLogoutButton()

        // 리사이클러뷰 설정
        // 어댑터 설정 (클릭 시 ResultActivity로 이동)
        adapter = DreamAdapter(
            dreamList,
            onItemClick = { dream ->
                showDreamDetail(dream)
            },
            onItemLongClick = { dream, position ->
                showEditDeleteDialog(dream, position)
            }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter


        // Firestore에서 꿈 목록 불러오기
        loadDreamsFromFirestore()

        // 꿈 작성 화면으로 이동
        fabAddDream.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val intent = Intent(this, DreamWriteActivity::class.java)
            intent.putExtra("uid", uid) // uid 전달
            startActivity(intent)
        }
    }
    private fun showDreamDetail(dream: Dream) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("dreamText", dream.dreamText)
            putExtra("emotion", dream.emotion)
            putExtra("gptResult", dream.gptInterpretation)
            putExtra("imageUrl", dream.imageUrl)
            putExtra("fromMain", true) // ✅ 출처 플래그 전달
        }
        startActivity(intent)
    }

    // RecyclerView 각 항목 수정 삭제
    private fun showEditDeleteDialog(dream: Dream, position: Int) {
        val options = arrayOf( "삭제")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // 삭제
                        deleteDreamFromFirestore(dream, position)
                    }
                }
            }
            .show()
    }


    // Firestore에서 꿈 목록 가져오기
    private fun loadDreamsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("dreams")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            // db 실시간 동기화
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val updatedList = mutableListOf<Dream>()
                for (doc in snapshots.documents) {
                    val dream = doc.toObject(Dream::class.java)
                    if (dream != null) {
                        // 꿈 id 추가
                        dream.id = doc.id
                        updatedList.add(dream)
                    }
                }


                // 최신 상태로 전체 교체
                dreamList.clear()
                dreamList.addAll(updatedList)
                adapter.notifyDataSetChanged()
            }
    }

    // fireBase 꿈 삭제
    private fun deleteDreamFromFirestore(dream: Dream, position: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Firestore에서 삭제
        db.collection("users").document(userId)
            .collection("dreams").document(dream.id) // 자동 생성된 dream id 찾아서 삭제
            .delete()
            .addOnSuccessListener {
                // 실시간 동기화 자동 반영
            }
            .addOnFailureListener {
                // 실패시 에러 표시
                android.widget.Toast.makeText(this, "삭제 실패", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}
