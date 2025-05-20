package com.example.dreamlog.viewmodel

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dreamlog.databinding.ActivityMainBinding
import com.example.dreamlog.model.Dream
import com.example.dreamlog.adapter.DreamAdapter
import com.example.dreamlog.adapter.DrawerAdapter
import com.example.dreamlog.R
import androidx.appcompat.app.ActionBarDrawerToggle

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DreamAdapter
    private lateinit var toggle: ActionBarDrawerToggle
    private val dreamList = mutableListOf<Dream>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 드로어(햄버거 메뉴) 설정
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // 리사이클러뷰 설정
        adapter = DreamAdapter(dreamList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Firestore에서 꿈 목록 불러오기
        loadDreamsFromFirestore()

        // DrawerAdapter 연동
        binding.navigationView.setNavigationItemSelectedListener(DrawerAdapter(this))

        // 꿈 작성 화면으로 이동
        binding.fabAddDream.setOnClickListener {
            val intent = Intent(this, DreamWriteActivity::class.java)
            startActivity(intent)
        }
    }

    // 햄버거 버튼 눌렀을 때 Drawer 열기 처리
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
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
                    val dream = document.toObject(Dream::class.java)
                    dreamList.add(dream)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
