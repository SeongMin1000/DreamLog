package com.example.dreamlog.adapter

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.dreamlog.R

class ToolbarAdapter(
    private val activity: AppCompatActivity,
    private val toolbar: Toolbar,
    private val drawerLayout: DrawerLayout
) {

    init {
        // 툴바를 액션바로 설정
        activity.setSupportActionBar(toolbar)

        // 타이틀 텍스트 색상 설정
        toolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.black_white_text))

        // 상태바 색상 설정
        activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.white_black_background)

        // 햄버거 토글 연결
        val toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        // 햄버거 아이콘 색상 설정
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(activity, R.color.black_white_text)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }
}
