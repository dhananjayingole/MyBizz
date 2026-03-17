package eu.tutorials.mybizz.Chatbot.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ✅ Change this to your PC's local IP when testing on a real device
    // For emulator use: http://10.0.2.2:8000/
    // For real device use: http://YOUR_PC_IP:8000/ e.g. http://192.168.1.5:8000/
    private const val BASE_URL = "http://10.0.2.2:8000/"
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // LLM can be slow, give it 60s
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }
}