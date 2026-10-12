package com.fahimkhan.ava.security.owner

import android.content.Context
import com.fahimkhan.ava.ai.memory.FaceMemory
import com.fahimkhan.ava.ai.memory.MemoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class UserTrustLevel { OWNER, TRUSTED, UNKNOWN, INTRUDER }

data class SecuritySession(
    val trustLevel: UserTrustLevel = UserTrustLevel.UNKNOWN,
    val personName: String = "",
    val sessionStart: Long = System.currentTimeMillis(),
    val isTemporary: Boolean = false,
    val expiresAt: Long = 0L
)

@Singleton
class OwnerSecuritySystem @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryRepo: MemoryRepository
) {
    private val _currentSession = MutableStateFlow(SecuritySession())
    val currentSession = _currentSession.asStateFlow()

    private val _isOwnerPresent = MutableStateFlow(false)
    val isOwnerPresent = _isOwnerPresent.asStateFlow()

    suspend fun onFaceRecognized(faceMemory: FaceMemory?) {
        if (faceMemory == null) { handleUnknownUser(); return }
        val trust = when (faceMemory.trustLevel) { 2 -> UserTrustLevel.OWNER; 1 -> UserTrustLevel.TRUSTED; else -> UserTrustLevel.UNKNOWN }
        _currentSession.value = SecuritySession(trustLevel = trust, personName = faceMemory.personName, isTemporary = false)
        _isOwnerPresent.value = trust == UserTrustLevel.OWNER
        if (trust == UserTrustLevel.OWNER) memoryRepo.logSecurityEvent("OWNER_RECOGNIZED", "INFO", "Owner ${faceMemory.personName} authenticated")
    }

    suspend fun grantTemporaryTrust(personName: String, durationMinutes: Int = 60) {
        val expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        _currentSession.value = SecuritySession(trustLevel = UserTrustLevel.TRUSTED, personName = personName, isTemporary = true, expiresAt = expiresAt)
        memoryRepo.logSecurityEvent("TEMP_TRUST_GRANTED", "INFO", "Temporary access granted to $personName for $durationMinutes minutes")
    }

    suspend fun revokeAccess() {
        val previous = _currentSession.value
        _currentSession.value = SecuritySession()
        _isOwnerPresent.value = false
        memoryRepo.logSecurityEvent("ACCESS_REVOKED", "INFO", "Session ended for ${previous.personName}")
    }

    private suspend fun handleUnknownUser() {
        _currentSession.value = SecuritySession(trustLevel = UserTrustLevel.UNKNOWN)
        _isOwnerPresent.value = false
        memoryRepo.logSecurityEvent("UNKNOWN_USER_DETECTED", "WARNING", "Unrecognized face detected")
    }

    suspend fun flagAsIntruder(imagePath: String = "") {
        _currentSession.value = SecuritySession(trustLevel = UserTrustLevel.INTRUDER)
        memoryRepo.logSecurityEvent("INTRUDER_DETECTED", "CRITICAL", "Possible intruder detected", imagePath)
    }

    fun canAccessSensitiveFeatures(): Boolean {
        val session = _currentSession.value
        if (session.isTemporary && System.currentTimeMillis() > session.expiresAt) return false
        return session.trustLevel == UserTrustLevel.OWNER || session.trustLevel == UserTrustLevel.TRUSTED
    }
}
