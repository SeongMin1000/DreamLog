package com.example.dreamlog.model

data class ImageGenerationResponse(
    val data: List<ImageData>
)

data class ImageData(
    val url: String
)