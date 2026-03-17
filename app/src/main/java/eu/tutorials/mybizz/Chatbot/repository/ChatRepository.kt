package eu.tutorials.mybizz.Chatbot.repository

import eu.tutorials.mybizz.Chatbot.data.RetrofitClient
import eu.tutorials.mybizz.Chatbot.model.ChatRequest
import eu.tutorials.mybizz.Chatbot.model.ChatResponse

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class ChatRepository {

    private val api = RetrofitClient.instance

    suspend fun sendMessage(query: String, sessionId: String? = null): Result<ChatResponse> {
        return try {
            val response = api.sendMessage(ChatRequest(query = query, sessionId = sessionId))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    503 -> "Chatbot not ready. Please load documents first."
                    500 -> "Server error. Please try again."
                    else -> "Error ${response.code()}: ${response.message()}"
                }
                Result.Error(errorMsg)
            }
        } catch (e: java.net.ConnectException) {
            Result.Error("Cannot connect to server. Make sure your API is running.")
        } catch (e: java.net.SocketTimeoutException) {
            Result.Error("Request timed out. The AI is taking too long to respond.")
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    suspend fun checkHealth(): Boolean {
        return try {
            val response = api.checkHealth()
            response.isSuccessful && response.body()?.vectorStoreReady == true
        } catch (e: Exception) {
            false
        }
    }
}