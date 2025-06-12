package com.example.dreamlog.model

data class User(
    val userEmail: String = "",
    val name: String = "",
    val comment: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = 0L
)

