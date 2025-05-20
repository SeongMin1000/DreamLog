package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
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
            val intent = Intent(this, DreamWriteActivity::class.java)
            startActivity(intent)
        }
    }



    // Firestore에서 꿈 목록 가져오기
    private fun loadDreamsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("dreams")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                dreamList.clear()
                for (document in result) {
                    val dream = document.toObject(com.example.dreamlog.model.Dream::class.java)
                    dreamList.add(dream)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
