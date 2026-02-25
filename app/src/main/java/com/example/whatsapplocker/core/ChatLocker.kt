package com.example.whatsapplocker.core

/**
 * ChatLocker provides policy hooks for WhatsApp/WhatsApp Business chat locking.
 *
 * Individual chat-level locking requires AccessibilityService based screen parsing,
 * which depends on OEM behavior and may break after app updates.
 * This module keeps the API ready for that extension while enforcing app-level lock now.
 */
class ChatLocker {

    fun isWhatsAppFamily(packageName: String): Boolean {
        return packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b"
    }

    fun shouldRequestChatScopeHint(packageName: String): Boolean {
        return isWhatsAppFamily(packageName)
    }
}
