package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dreamlog.R
import com.example.dreamlog.adapter.DrawerAdapter
import com.example.dreamlog.adapter.DreamAdapter
import com.example.dreamlog.adapter.ToolbarAdapter
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


        // 툴바 + 햄버거 메뉴 설정
        ToolbarAdapter(this, toolbar, drawerLayout)

        // 드로어 어댑터 설정 및 색상 적용
        DrawerAdapter(this, navigationView)

        // 리사이클러뷰 설정
        adapter = DreamAdapter(dreamList)
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

        // 로그아웃 버튼 drawerAdapter에서 제외
        val logoutBtn = findViewById<Button>(R.id.menu_logout)
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

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
                        updatedList.add(dream)
                    }
                }

                // 최신 상태로 전체 교체
                dreamList.clear()
                dreamList.addAll(updatedList)
                adapter.notifyDataSetChanged()
            }
    }
}
