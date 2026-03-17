package eu.tutorials.mybizz.Chatbot.model

import com.google.gson.annotations.SerializedName

// ── Request ───────────────────────────────────────────────
data class ChatRequest(
    @SerializedName("query") val query: String,
    @SerializedName("session_id") val sessionId: String? = null
)

// ── Response ──────────────────────────────────────────────
data class ChatResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("sources") val sources: List<SourceDocument>,
    @SerializedName("query") val query: String,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("timestamp") val timestamp: String
)

data class SourceDocument(
    @SerializedName("sheet_name") val sheetName: String,
    @SerializedName("row_number") val rowNumber: Int,
    @SerializedName("content") val content: String
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("version") val version: String,
    @SerializedName("vector_store_ready") val vectorStoreReady: Boolean
)

// ── UI State ──────────────────────────────────────────────
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val sources: List<SourceDocument> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Error(val message: String) : ChatUiState()
    object Success : ChatUiState()
}