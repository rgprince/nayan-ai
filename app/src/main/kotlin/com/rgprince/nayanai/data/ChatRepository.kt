package com.rgprince.nayanai.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ChatDatabase::class.java,
        "nayan_chat_db"
    ).build()
    
    private val chatDao = database.chatDao()
    
    fun getAllChatSessions(): Flow<List<ChatSession>> = chatDao.getAllChatSessions()
    
    fun getMessagesForSession(sessionId: Long): Flow<List<Message>> = 
        chatDao.getMessagesForSession(sessionId)
    
    suspend fun createChatSession(modelName: String, firstMessage: String? = null): Long {
        val title = firstMessage?.let { generateTitle(it) } ?: generateDefaultTitle()
        val session = ChatSession(
            title = title,
            modelName = modelName
        )
        return chatDao.insertChatSession(session)
    }
    
    suspend fun addMessage(sessionId: Long, content: String, isUser: Boolean) {
        val message = Message(
            chatSessionId = sessionId,
            content = content,
            isUser = isUser
        )
        chatDao.insertMessage(message)
        
        // Update last message timestamp and message count
        val session = chatDao.getChatSession(sessionId)
        session?.let {
            val updatedSession = it.copy(
                lastMessageAt = System.currentTimeMillis(),
                messageCount = it.messageCount + 1
            )
            chatDao.updateChatSession(updatedSession)
            
            // Update title with first user message if still using default title
            if (isUser && it.messageCount == 0) {
                chatDao.updateChatTitle(sessionId, generateTitle(content))
            }
        }
    }
    
    suspend fun updateChatTitle(sessionId: Long, newTitle: String) {
        chatDao.updateChatTitle(sessionId, newTitle)
    }
    
    suspend fun deleteChatSession(session: ChatSession) {
        chatDao.deleteChatSession(session)
    }
    
    private fun generateTitle(firstMessage: String): String {
        // Take first 40 characters or first sentence
        val trimmed = firstMessage.trim()
        val firstSentence = trimmed.split(Regex("[.!?]")).firstOrNull() ?: trimmed
        return if (firstSentence.length > 40) {
            firstSentence.take(40) + "..."
        } else {
            firstSentence
        }
    }
    
    private fun generateDefaultTitle(): String {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return "Chat ${dateFormat.format(Date())}"
    }
}
