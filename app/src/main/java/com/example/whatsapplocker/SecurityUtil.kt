package com.example.whatsapplocker

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtil {

    private const val PREFS_FILE = "locker_secured_prefs"
    private const val KEY_PIN = "saved_pin"
    private const val KEY_LOCKED_APPS = "locked_apps"
    private const val KEY_LOCK_ENABLED = "lock_enabled"

    private var sharedPreferences: SharedPreferences? = null

    // In-memory unlock state so we don't lock immediately after unlocking
    private var temporarilyUnlockedApp: String? = null
    private var unlockedTimestamp: Long = 0

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun getPrefs(): SharedPreferences {
        return sharedPreferences ?: throw IllegalStateException("SecurityUtil not initialized. Call init() first.")
    }

    fun isLockEnabled(): Boolean = getPrefs().getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        getPrefs().edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun hasPin(): Boolean = getPrefs().contains(KEY_PIN)

    fun savePin(pin: String) {
        getPrefs().edit().putString(KEY_PIN, pin).apply()
    }

    fun checkPin(pin: String): Boolean {
        val savedPin = getPrefs().getString(KEY_PIN, null)
        return savedPin == pin
    }

    fun getLockedApps(): Set<String> {
        return getPrefs().getStringSet(KEY_LOCKED_APPS, setOf("com.whatsapp.w4b")) ?: setOf("com.whatsapp.w4b")
    }

    fun addLockedApp(packageName: String) {
        val currentSet = getLockedApps().toMutableSet()
        currentSet.add(packageName)
        getPrefs().edit().putStringSet(KEY_LOCKED_APPS, currentSet).apply()
    }

    fun removeLockedApp(packageName: String) {
        val currentSet = getLockedApps().toMutableSet()
        currentSet.remove(packageName)
        getPrefs().edit().putStringSet(KEY_LOCKED_APPS, currentSet).apply()
    }

    fun isAppLocked(packageName: String): Boolean {
        return isLockEnabled() && getLockedApps().contains(packageName)
    }

    fun setTemporarilyUnlocked(packageName: String) {
        temporarilyUnlockedApp = packageName
        unlockedTimestamp = System.currentTimeMillis()
    }

    fun clearTemporarilyUnlocked() {
        temporarilyUnlockedApp = null
        unlockedTimestamp = 0
    }

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        // App is unlocked if it's the specific unlocked package and the unlock wasn't too long ago
        // E.g., timeout of 5 minutes (300000ms) or we can just rely on the service clearing it when the app changes.
        return temporarilyUnlockedApp == packageName
    }
}
