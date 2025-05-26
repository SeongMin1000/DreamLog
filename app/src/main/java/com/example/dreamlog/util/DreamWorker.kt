package com.example.dreamlog.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.api.ImageGenRetrofitInstance
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Dream
import com.example.dreamlog.model.ImageGenerationRequest
import com.example.dreamlog.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DreamWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val dreamText = inputData.getString("dreamText") ?: return Result.failure()
        val emotion = inputData.getString("emotion") ?: return Result.failure()
        val gptInterpretation = inputData.getString("gptInterpretation") ?: return Result.failure()
        val docId = inputData.getString("docId") ?: return Result.failure() // ★ 추가

        return try {
            // 1. GPT로 이미지 프롬프트 생성
            val promptRequest = ChatRequest(
                messages = listOf(
                    Message(
                        role = "system",
                        content = "너는 이미지 디자이너야. 사용자가 입력한 꿈의 심리적 해석과 감정을 바탕으로 이미지 생성 AI에 넣을 프롬프트 문장을 만들어줘. 핵심만 요약해서 최대한 짧은 문장으로."
                    ),
                    Message(
                        role = "user",
                        content = "감정: $emotion\n해석: $gptInterpretation\n이 내용을 이미지로 형상화할 수 있는 프롬프트를 영어로 작성해줘. 묘사 중심으로."
                    )
                )
            )
            val promptResponse = GptRetrofitInstance.api.getChatCompletion(promptRequest)
            val imagePrompt =
                promptResponse.choices.firstOrNull()?.message?.content ?: return Result.failure()

            // 2. 이미지 생성 API 호출
            val imageResponse = ImageGenRetrofitInstance.api.generateImage(
                ImageGenerationRequest(prompt = imagePrompt)
            )
            val imageUrl = imageResponse.data.firstOrNull()?.url ?: ""

            // 3. Dream 객체 생성 (timestamp는 서버 시간 기준)
            val dream = Dream(
                dreamText = dreamText,
                emotion = emotion,
                gptInterpretation = gptInterpretation,
                imageUrl = imageUrl,
                timestamp = Timestamp.now()
            )

            // 4. Firestore에 docId로 저장 (덮어쓰기)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dreams")
                .document(docId)
                .set(dream)
                .await()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
