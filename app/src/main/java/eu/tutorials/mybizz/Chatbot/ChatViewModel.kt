package eu.tutorials.mybizz.Chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.mybizz.Chatbot.model.ChatMessage
import eu.tutorials.mybizz.Chatbot.model.ChatUiState
import eu.tutorials.mybizz.Chatbot.model.SourceDocument
import eu.tutorials.mybizz.Chatbot.repository.ChatRepository
import eu.tutorials.mybizz.Chatbot.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val sessionId = UUID.randomUUID().toString()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        addBotMessage(
            "👋 Hi! I'm your MyBizz Assistant.\n\nAsk me anything about your bills, tenants, or payments.\n\n" +
                    "**Examples:**\n• Show all unpaid bills\n• Total amount paid in March\n• List all Worker Payments"
        )
    }

    fun sendMessage(query: String) {
        if (query.isBlank() || _isTyping.value) return

        addUserMessage(query)
        _isTyping.value = true
        _uiState.value = ChatUiState.Loading

        viewModelScope.launch {
            when (val result = repository.sendMessage(query, sessionId)) {
                is Result.Success -> {
                    _isTyping.value = false
                    _uiState.value = ChatUiState.Success
                    addBotMessage(
                        content = result.data.answer,
                        sources = result.data.sources
                    )
                }
                is Result.Error -> {
                    _isTyping.value = false
                    _uiState.value = ChatUiState.Error(result.message)
                    addErrorMessage(result.message)
                }
            }
        }
    }

    private fun addUserMessage(content: String) {
        _messages.value = _messages.value + ChatMessage(
            content = content,
            isFromUser = true
        )
    }

    private fun addBotMessage(
        content: String,
        sources: List<SourceDocument> = emptyList()
    ) {
        _messages.value = _messages.value + ChatMessage(
            content = content,
            isFromUser = false,
            sources = sources
        )
    }

    private fun addErrorMessage(error: String) {
        _messages.value = _messages.value + ChatMessage(
            content = "❌ $error",
            isFromUser = false,
            isError = true
        )
    }

    fun clearChat() {
        _messages.value = emptyList()
        addBotMessage("Chat cleared! How can I help you?")
    }
}