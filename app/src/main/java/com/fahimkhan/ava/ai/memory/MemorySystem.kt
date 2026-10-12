package com.fahimkhan.ava.ai.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName = "short_term_memory")
data class ShortTermMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String, val category: String = "general",
    val timestamp: Long = System.currentTimeMillis(), val importance: Int = 1
)

@Entity(tableName = "long_term_memory")
data class LongTermMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String, val value: String, val category: String = "general",
    val timestamp: Long = System.currentTimeMillis(), val accessCount: Int = 0
)

@Entity(tableName = "conversation_memory")
data class ConversationMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMessage: String, val avaResponse: String,
    val emotion: String = "neutral", val timestamp: Long = System.currentTimeMillis(), val sessionId: String = ""
)

@Entity(tableName = "face_memory")
data class FaceMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String, val relationship: String = "unknown", val notes: String = "",
    val faceEmbedding: String = "", val imageUriList: String = "",
    val trustLevel: Int = 0, val registeredAt: Long = System.currentTimeMillis(), val lastSeenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_log")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val event: String, val severity: String = "INFO",
    val description: String = "", val imagePath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao interface ShortTermMemoryDao {
    @Insert suspend fun insert(m: ShortTermMemory): Long
    @Query("SELECT * FROM short_term_memory ORDER BY timestamp DESC LIMIT 50") fun getRecent(): Flow<List<ShortTermMemory>>
    @Query("SELECT * FROM short_term_memory ORDER BY timestamp DESC LIMIT :n") suspend fun getRecentN(n: Int): List<ShortTermMemory>
    @Query("DELETE FROM short_term_memory") suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM short_term_memory") suspend fun count(): Int
}

@Dao interface LongTermMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: LongTermMemory): Long
    @Query("SELECT * FROM long_term_memory WHERE key = :key LIMIT 1") suspend fun getByKey(key: String): LongTermMemory?
    @Query("SELECT * FROM long_term_memory ORDER BY accessCount DESC") fun getAll(): Flow<List<LongTermMemory>>
    @Query("UPDATE long_term_memory SET accessCount = accessCount + 1 WHERE key = :key") suspend fun incrementAccess(key: String)
    @Query("DELETE FROM long_term_memory") suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM long_term_memory") suspend fun count(): Int
}

@Dao interface ConversationMemoryDao {
    @Insert suspend fun insert(m: ConversationMemory): Long
    @Query("SELECT * FROM conversation_memory ORDER BY timestamp DESC LIMIT 100") fun getRecent(): Flow<List<ConversationMemory>>
    @Query("SELECT * FROM conversation_memory ORDER BY timestamp DESC LIMIT :n") suspend fun getRecentN(n: Int): List<ConversationMemory>
    @Query("DELETE FROM conversation_memory") suspend fun clearAll()
    @Query("SELECT COUNT(*) FROM conversation_memory") suspend fun count(): Int
}

@Dao interface FaceMemoryDao {
    @Insert suspend fun insert(f: FaceMemory): Long
    @Update suspend fun update(f: FaceMemory)
    @Query("SELECT * FROM face_memory") fun getAll(): Flow<List<FaceMemory>>
    @Query("SELECT * FROM face_memory WHERE trustLevel >= :minTrust") suspend fun getTrusted(minTrust: Int = 1): List<FaceMemory>
    @Query("SELECT * FROM face_memory WHERE trustLevel = 2 LIMIT 1") suspend fun getOwner(): FaceMemory?
    @Query("SELECT COUNT(*) FROM face_memory") suspend fun count(): Int
    @Delete suspend fun delete(f: FaceMemory)
}

@Dao interface SecurityLogDao {
    @Insert suspend fun insert(l: SecurityLog): Long
    @Query("SELECT * FROM security_log ORDER BY timestamp DESC LIMIT 200") fun getRecent(): Flow<List<SecurityLog>>
    @Query("DELETE FROM security_log") suspend fun clearAll()
}

@Database(
    entities = [ShortTermMemory::class, LongTermMemory::class, ConversationMemory::class, FaceMemory::class, SecurityLog::class],
    version = 1, exportSchema = false
)
abstract class AvaDatabase : RoomDatabase() {
    abstract fun shortTermDao(): ShortTermMemoryDao
    abstract fun longTermDao(): LongTermMemoryDao
    abstract fun conversationDao(): ConversationMemoryDao
    abstract fun faceMemoryDao(): FaceMemoryDao
    abstract fun securityLogDao(): SecurityLogDao
}

@Singleton
class MemoryRepository @Inject constructor(val db: AvaDatabase) {
    suspend fun saveConversation(userMsg: String, avaResponse: String) {
        db.conversationDao().insert(ConversationMemory(userMessage = userMsg, avaResponse = avaResponse))
        db.shortTermDao().insert(ShortTermMemory(content = "User said: $userMsg", category = "conversation"))
    }
    suspend fun saveToLongTerm(key: String, value: String, category: String = "general") = db.longTermDao().insert(LongTermMemory(key = key, value = value, category = category))
    suspend fun recallFromLongTerm(key: String): String? { val m = db.longTermDao().getByKey(key); if (m != null) db.longTermDao().incrementAccess(key); return m?.value }
    suspend fun getRecentContextSummary(): String { val c = db.conversationDao().getRecentN(5); if (c.isEmpty()) return "No previous conversations."; return c.reversed().joinToString("\n") { "User: ${it.userMessage}\nAVA: ${it.avaResponse}" } }
    suspend fun registerFace(name: String, relationship: String, embedding: String, imageUri: String, trustLevel: Int = 1) = db.faceMemoryDao().insert(FaceMemory(personName = name, relationship = relationship, faceEmbedding = embedding, imageUriList = imageUri, trustLevel = trustLevel))
    suspend fun getOwnerProfile() = db.faceMemoryDao().getOwner()
    suspend fun getTrustedPeople() = db.faceMemoryDao().getTrusted()
    suspend fun logSecurityEvent(event: String, severity: String = "INFO", description: String = "", imagePath: String = "") = db.securityLogDao().insert(SecurityLog(event = event, severity = severity, description = description, imagePath = imagePath))
    suspend fun getStats() = Triple(db.shortTermDao().count(), db.longTermDao().count(), db.conversationDao().count())
    suspend fun clearAll() { db.shortTermDao().clearAll(); db.longTermDao().clearAll(); db.conversationDao().clearAll() }
}
