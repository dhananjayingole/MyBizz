package eu.tutorials.mybizz.Chatbot.data

import eu.tutorials.mybizz.Chatbot.model.ChatRequest
import eu.tutorials.mybizz.Chatbot.model.ChatResponse
import eu.tutorials.mybizz.Chatbot.model.HealthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ChatApiService {

    @POST("api/v1/chat/query")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @GET("api/v1/health/")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("api/v1/documents/stats")
    suspend fun getStats(): Response<Any>
}