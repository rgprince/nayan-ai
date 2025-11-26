package com.rgprince.nayanai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val modelName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["chatSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatSessionId")]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatSessionId: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastMessageAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSession>>
    
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getChatSession(sessionId: Long): ChatSession?
    
    @Query("SELECT * FROM messages WHERE chatSessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<Message>>
    
    @Insert
    suspend fun insertChatSession(session: ChatSession): Long
    
    @Insert
    suspend fun insertMessage(message: Message)
    
    @Update
    suspend fun updateChatSession(session: ChatSession)
    
    @Delete
    suspend fun deleteChatSession(session: ChatSession)
    
    @Query("DELETE FROM messages WHERE chatSessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)
    
    @Query("UPDATE chat_sessions SET title = :newTitle WHERE id = :sessionId")
    suspend fun updateChatTitle(sessionId: Long, newTitle: String)
}

@Database(
    entities = [ChatSession::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
