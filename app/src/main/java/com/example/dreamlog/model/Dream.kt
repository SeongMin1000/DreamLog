package com.example.dreamlog.model

import com.google.firebase.Timestamp

data class Dream(
    val dreamText: String = "",           // 꿈 내용
    val emotion: String = "",             // 감정 분석 결과
    val gptInterpretation: String = "",   // gpt 분석 결과
    val imageUrl: String = "",            // 이미지 생성 경로
    val timestamp: Timestamp = Timestamp.now() // 생성 일자 타임스탬프 타입에 맞게 수정
)
