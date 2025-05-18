package com.example.dreamlog.model

data class Dream(
    val dreamText: String = "",           // 꿈 내용
    val emotion: String = "",             // 감정 분석 결과
    val gptInterpretation: String = "",   // gpt 분석 결과
    val imageUrl: String = "",            // 이미지 생성 경로
    val timestamp: Long = 0L              // 생성 일자
)
