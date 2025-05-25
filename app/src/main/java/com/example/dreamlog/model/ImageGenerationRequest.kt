package com.example.dreamlog.model

data class ImageGenerationRequest (
    val prompt: String = "",
    val n: Int = 1,
    val size: String = "512x512"
)