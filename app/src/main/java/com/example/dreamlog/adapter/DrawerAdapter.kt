package com.example.dreamlog.adapter

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.MenuItem
import com.example.dreamlog.R
import com.example.dreamlog.viewmodel.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat

class DrawerAdapter(
    private val context: Context,
    private val navigationView: NavigationView
) : NavigationView.OnNavigationItemSelectedListener {

    init {
        // 드로어 메뉴 색상 설정 여기서 한 번만 처리
        val color = ContextCompat.getColor(context, R.color.black_white_text)
        navigationView.itemIconTintList = ColorStateList.valueOf(color)
        navigationView.itemTextColor = ColorStateList.valueOf(color)

        // 리스너 등록까지 여기서 처리
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> { /* 홈 처리 */ true }
            R.id.menu_settings -> { /* 설정 처리 */ true }
            // menu_logout은 NavigationView 메뉴에서만 관리
            else -> false
        }
    }
}
