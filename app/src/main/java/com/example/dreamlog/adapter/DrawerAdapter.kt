package com.example.dreamlog.adapter

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.example.dreamlog.viewmodel.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class DrawerAdapter(private val context: Context) : NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.example.dreamlog.R.id.menu_logout -> {
                FirebaseAuth.getInstance().signOut()
                context.startActivity(Intent(context, LoginActivity::class.java))
                true
            }
            else -> false
        }
    }
}
