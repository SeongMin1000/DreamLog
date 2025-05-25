package com.example.dreamlog.api

import com.example.dreamlog.BuildConfig
import com.example.dreamlog.model.ChatRequest
import com.example.dreamlog.model.ChatResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// 🔹 GPT API 인터페이스 정의 (코루틴 기반)
interface GptApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(@Body request: ChatRequest): ChatResponse
}

// 🔹 Retrofit 인스턴스
object GptRetrofitInstance {
    private const val BASE_URL = "https://api.openai.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain: Interceptor.Chain ->
            val newRequest: Request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .build()
            chain.proceed(newRequest)
        }
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: GptApi by lazy {
        retrofit.create(GptApi::class.java)
    }
}

fun preprocessDreamText(rawText: String): String {
    return rawText
        .trim()
        .replace(Regex("[\\r\\n]+"), " ")  // 줄바꿈 → 공백
        .replace(Regex("[^ㄱ-ㅎ가-힣a-zA-Z0-9.,?!\\s]"), "") // 특수문자 제거 (이모지 등)
        .replace(Regex("\\s{2,}"), " ") // 다중 공백 → 하나로
}
