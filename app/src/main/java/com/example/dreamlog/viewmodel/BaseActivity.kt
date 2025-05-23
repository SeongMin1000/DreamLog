package com.example.dreamlog.viewmodel

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.dreamlog.R
import com.example.dreamlog.adapter.DrawerAdapter
import com.example.dreamlog.adapter.ToolbarAdapter
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setStatusBarTextColor()
        setupLogoutButton()
    }

    // 테마 모드에 따른 상태바 색상 반전
    private fun setStatusBarTextColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isDarkMode = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            window.decorView.systemUiVisibility = if (isDarkMode) {
                0
            } else {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    // 로그아웃 버튼 공통 리스너
    protected fun setupLogoutButton() {
        val logoutBtn = findViewById<Button?>(R.id.menu_logout)
        logoutBtn?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // 공통 툴바+드로어 설정 함수
    protected fun setupToolbarAndDrawer(
        toolbar: Toolbar,
        drawerLayout: DrawerLayout,
        navigationView: NavigationView
    ) {
        // 툴바+햄버거 메뉴
        ToolbarAdapter(this, toolbar, drawerLayout)
        // 드로어 메뉴(어댑터)
        DrawerAdapter(this, navigationView)
    }
}
