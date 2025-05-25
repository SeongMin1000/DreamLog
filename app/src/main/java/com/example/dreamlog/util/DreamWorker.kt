package com.example.dreamlog.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.dreamlog.api.GptRetrofitInstance
import com.example.dreamlog.api.ImageGenRetrofitInstance
import com.example.dreamlog.model.Dream
import com.example.dreamlog.model.ImageGenerationRequest
import com.example.dreamlog.model.ImageGenerationResponse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.Message


class DreamWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val dreamText = inputData.getString("dreamText") ?: return Result.failure()
        val emotion = inputData.getString("emotion") ?: return Result.failure()
        val gptInterpretation = inputData.getString("gptInterpretation") ?: return Result.failure()

        return try {
            // 1. GPT로 프롬프트 생성 요청
            val promptRequest = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "너는 이미지 디자이너야. 사용자가 입력한 꿈의 심리적 해석과 감정을 바탕으로 이미지 생성 AI에 넣을 프롬프트 문장을 만들어줘. 핵심만 요약해서 최대한 짧은 문장으로."),
                    Message(role = "user", content = "감정: $emotion\n해석: $gptInterpretation\n이 내용을 이미지로 형상화할 수 있는 프롬프트를 영어로 작성해줘. 묘사 중심으로.")
                )
            )

            val promptResponse = GptRetrofitInstance.api.getChatCompletion(promptRequest)
            val imagePrompt = promptResponse.choices.firstOrNull()?.message?.content ?: return Result.failure()

            // 2. 이미지 생성
            val imageResponse = ImageGenRetrofitInstance.api.generateImage(
                ImageGenerationRequest(prompt = imagePrompt)
            )
            val imageUrl = imageResponse.data.firstOrNull()?.url ?: ""

            // 3. Dream 객체 생성
            val dream = Dream(
                dreamText = dreamText,
                emotion = emotion,
                gptInterpretation = gptInterpretation,
                imageUrl = imageUrl,
                timestamp = Timestamp.now()
            )

            // 4. Firebase Firestore에 저장
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dreams")
                .add(dream)
                .await()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}


