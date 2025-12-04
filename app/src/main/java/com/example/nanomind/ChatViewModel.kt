package com.example.nanomind

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.nehuatl.llamacpp.LlamaHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _statusMessage = MutableStateFlow<String>("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow<Boolean>(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + viewModelJob)

    private val _llmFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val llmFlow = _llmFlow.asSharedFlow()

    private val llamaHelper by lazy {
        LlamaHelper(
            contentResolver = contentResolver,
            scope = scope,
            sharedFlow = _llmFlow
        )
    }

    companion object {
        private const val SYSTEM_PROMPT = "<|im_start|>system\nYou are NanoMind, a helpful, intelligent, and efficient AI assistant. You provide clear, concise, and accurate answers to any questions. You are knowledgeable across a wide range of topics including science, technology, history, arts, and general knowledge. Always be helpful, respectful, and provide well-structured responses.<|im_end|>\n"
    }

    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            android.util.Log.d("NanoMind", "=== STARTING MODEL LOAD ===")
            android.util.Log.d("NanoMind", "Model path: $modelPath")

            try {
                _statusMessage.value = "Loading Model..."

                // Load the model using LlamaHelper
                // Note: modelPath can be either a file path or content:// URI
                // The LlamaHelper will handle ContentResolver access internally
                android.util.Log.d("NanoMind", "Calling llamaHelper.load()...")
                llamaHelper.load(
                    path = modelPath,
                    contextLength = 2048
                ) { contextId ->
                    android.util.Log.d("NanoMind", "✓ Model loaded successfully! Context ID: $contextId")
                    _statusMessage.value = "Model Ready ✓"
                }

                // Start listening to LLM events
                viewModelScope.launch {
                    llmFlow.collect { event ->
                        when (event) {
                            is LlamaHelper.LLMEvent.Loaded -> {
                                android.util.Log.d("NanoMind", "Model loaded: ${event.path}")
                            }
                            is LlamaHelper.LLMEvent.Error -> {
                                android.util.Log.e("NanoMind", "LLM Error during load: ${event.message}")
                                _statusMessage.value = "Error: ${event.message}"
                            }
                            else -> {
                                // Other events handled in sendMessage
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("NanoMind", "ERROR: Exception - ${e.message}", e)
                _statusMessage.value = "Error Loading Model: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    private var currentCollectionJob: Job? = null

    fun sendMessage(userPrompt: String) {
        if (userPrompt.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            android.util.Log.d("NanoMind", "=== STARTING MESSAGE SEND ===")
            android.util.Log.d("NanoMind", "User prompt: $userPrompt")

            try {
                _isGenerating.value = true
                _statusMessage.value = "Generating..."

                // Cancel any previous collection job
                currentCollectionJob?.cancel()

                // Add user message to chat
                val userMessage = Message(text = userPrompt, isUser = true)
                _messages.value = _messages.value + userMessage

                // Create assistant message placeholder
                val assistantMessage = Message(text = "", isUser = false)
                val messageIndex = _messages.value.size
                _messages.value = _messages.value + assistantMessage

                // Format the full prompt
                val fullPrompt = SYSTEM_PROMPT + "<|im_start|>user\n$userPrompt<|im_end|>\n<|im_start|>assistant\n"
                android.util.Log.d("NanoMind", "Full prompt length: ${fullPrompt.length}")

                // Record start time
                val startTime = System.currentTimeMillis()

                // Start prediction
                var accumulatedText = ""
                llamaHelper.predict(fullPrompt)

                // Collect events from the flow - only for this message
                currentCollectionJob = viewModelScope.launch {
                    llmFlow.collect { event ->
                        when (event) {
                            is LlamaHelper.LLMEvent.Ongoing -> {
                                // A new token has been generated
                                accumulatedText += event.word
                                // Update the assistant message with accumulated text
                                val updatedMessages = _messages.value.toMutableList()
                                updatedMessages[messageIndex] = assistantMessage.copy(text = accumulatedText)
                                _messages.value = updatedMessages
                            }
                            is LlamaHelper.LLMEvent.Done -> {
                                android.util.Log.d("NanoMind", "Generation completed")
                                val endTime = System.currentTimeMillis()
                                val inferenceTime = endTime - startTime
                                android.util.Log.d("NanoMind", "Inference completed in ${inferenceTime}ms")
                                _statusMessage.value = "Inference Time: ${inferenceTime} ms"
                                _isGenerating.value = false
                                // Stop collecting after Done event
                                currentCollectionJob?.cancel()
                            }
                            is LlamaHelper.LLMEvent.Error -> {
                                android.util.Log.e("NanoMind", "Generation error: ${event.message}")
                                _statusMessage.value = "Error: ${event.message}"
                                _isGenerating.value = false
                                // Remove the empty assistant message on error
                                _messages.value = _messages.value.dropLast(1)
                                currentCollectionJob?.cancel()
                            }
                            else -> {
                                // Ignore other events during prediction
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("NanoMind", "ERROR in sendMessage: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
                // Remove the empty assistant message on error
                _messages.value = _messages.value.dropLast(1)
                _isGenerating.value = false
            } finally {
                android.util.Log.d("NanoMind", "=== MESSAGE SEND COMPLETE ===")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llamaHelper.abort()
        llamaHelper.release()
        viewModelJob.cancel()
    }
}
