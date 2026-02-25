package com.example.whatsapplocker.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    enum class LockMethod { PIN, PASSWORD, PATTERN }

    private const val PREFS_FILE = "locker_secure_prefs"
    private const val KEY_CREDENTIAL_HASH = "credential_hash"
    private const val KEY_CREDENTIAL_SALT = "credential_salt"
    private const val KEY_LOCK_METHOD = "lock_method"
    private const val KEY_TIMEOUT_MS = "timeout_ms"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_LOCKED_APPS = "locked_apps"

    private var prefs: SharedPreferences? = null

    private var temporarilyUnlockedApp: String? = null
    private var unlockTimestamp: Long = 0L

    fun init(context: Context) {
        if (prefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: error("SecurityUtils.init must be called first")
    }

    fun setCredential(rawCredential: String, method: LockMethod) {
        val salt = SecureRandom().generateSeed(16)
        val hash = hashCredential(rawCredential, salt)
        getPrefs().edit()
            .putString(KEY_CREDENTIAL_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_CREDENTIAL_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_LOCK_METHOD, method.name)
            .apply()
    }

    fun verifyCredential(rawCredential: String): Boolean {
        val storedHash = getPrefs().getString(KEY_CREDENTIAL_HASH, null) ?: return false
        val storedSalt = getPrefs().getString(KEY_CREDENTIAL_SALT, null) ?: return false

        val saltBytes = Base64.decode(storedSalt, Base64.NO_WRAP)
        val hash = hashCredential(rawCredential, saltBytes)
        val encoded = Base64.encodeToString(hash, Base64.NO_WRAP)
        return encoded == storedHash
    }

    fun hasCredential(): Boolean = getPrefs().contains(KEY_CREDENTIAL_HASH)

    fun getLockMethod(): LockMethod {
        val value = getPrefs().getString(KEY_LOCK_METHOD, LockMethod.PIN.name) ?: LockMethod.PIN.name
        return LockMethod.valueOf(value)
    }

    fun setTimeoutMs(timeoutMs: Long) {
        getPrefs().edit().putLong(KEY_TIMEOUT_MS, timeoutMs).apply()
    }

    fun getTimeoutMs(): Long = getPrefs().getLong(KEY_TIMEOUT_MS, 30_000L)

    fun setLockEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun isLockEnabled(): Boolean = getPrefs().getBoolean(KEY_LOCK_ENABLED, false)

    fun getLockedApps(): Set<String> {
        return getPrefs().getStringSet(KEY_LOCKED_APPS, setOf("com.whatsapp.w4b")) ?: setOf("com.whatsapp.w4b")
    }

    fun addLockedApp(packageName: String) {
        val updated = getLockedApps().toMutableSet().apply { add(packageName) }
        getPrefs().edit().putStringSet(KEY_LOCKED_APPS, updated).apply()
    }

    fun removeLockedApp(packageName: String) {
        val updated = getLockedApps().toMutableSet().apply { remove(packageName) }
        getPrefs().edit().putStringSet(KEY_LOCKED_APPS, updated).apply()
    }

    fun markAppUnlocked(packageName: String) {
        temporarilyUnlockedApp = packageName
        unlockTimestamp = System.currentTimeMillis()
    }

    fun clearTemporaryUnlock() {
        temporarilyUnlockedApp = null
        unlockTimestamp = 0L
    }

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val withinTimeout = (System.currentTimeMillis() - unlockTimestamp) <= getTimeoutMs()
        return temporarilyUnlockedApp == packageName && withinTimeout
    }

    private fun hashCredential(credential: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(credential.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }
}
