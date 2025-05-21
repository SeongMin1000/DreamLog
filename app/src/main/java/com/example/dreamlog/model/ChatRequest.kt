package com.example.dreamlog.model

// ChatRequest.kt
data class Message(val role: String, val content: String)

data class ChatRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>
)