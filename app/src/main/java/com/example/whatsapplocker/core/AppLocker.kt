package com.example.whatsapplocker.core

import com.example.whatsapplocker.security.SecurityUtils

class AppLocker {

    fun shouldLock(packageName: String): Boolean {
        if (!SecurityUtils.isLockEnabled()) return false
        if (!SecurityUtils.getLockedApps().contains(packageName)) return false
        return !SecurityUtils.isTemporarilyUnlocked(packageName)
    }

    fun onAppUnlocked(packageName: String) {
        SecurityUtils.markAppUnlocked(packageName)
    }

    fun onForegroundAppChanged(newPackageName: String, oldPackageName: String?) {
        if (oldPackageName != null && newPackageName != oldPackageName) {
            if (SecurityUtils.isTemporarilyUnlocked(oldPackageName)) {
                SecurityUtils.clearTemporaryUnlock()
            }
        }
    }
}
