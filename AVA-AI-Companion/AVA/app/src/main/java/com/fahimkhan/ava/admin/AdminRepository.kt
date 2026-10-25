package com.fahimkhan.ava.admin

import android.content.Context
import com.fahimkhan.ava.ai.memory.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton

data class SystemStats(val shortTermCount: Int = 0, val longTermCount: Int = 0, val faceProfileCount: Int = 0, val conversationCount: Int = 0)

@Singleton
class AdminRepository @Inject constructor(private val memory: MemoryRepository, private val context: Context) {
    private val defaultPassword = "ava2024"
    fun verifyPassword(input: String): Boolean {
        val stored = context.getSharedPreferences("ava_admin", Context.MODE_PRIVATE).getString("admin_pass", defaultPassword) ?: defaultPassword
        return input == stored
    }
    fun updatePassword(newPassword: String) = context.getSharedPreferences("ava_admin", Context.MODE_PRIVATE).edit().putString("admin_pass", newPassword).apply()
    suspend fun getSystemStats(): SystemStats { val (st, lt, conv) = memory.getStats(); return SystemStats(st, lt, 0, conv) }
    suspend fun clearAllMemory() = memory.clearAll()
}
