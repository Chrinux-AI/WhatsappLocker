package com.example.whatsapplocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.whatsapplocker.core.AppLocker
import com.example.whatsapplocker.core.ChatLocker
import com.example.whatsapplocker.security.SecurityUtils

class LockService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val appLocker = AppLocker()
    private val chatLocker = ChatLocker()

    private lateinit var usageStatsManager: UsageStatsManager
    private var lastForegroundPackage: String? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            pollForegroundApp()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        SecurityUtils.init(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(monitorRunnable)
        if (SecurityUtils.isLockEnabled()) {
            handler.post(monitorRunnable)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    private fun pollForegroundApp() {
        val foregroundPackage = getTopPackage() ?: return
        if (foregroundPackage == packageName) return

        appLocker.onForegroundAppChanged(foregroundPackage, lastForegroundPackage)
        lastForegroundPackage = foregroundPackage

        val shouldLock = appLocker.shouldLock(foregroundPackage)
        if (shouldLock) {
            if (chatLocker.shouldRequestChatScopeHint(foregroundPackage)) {
                // Hook is reserved for future per-chat accessibility extension.
            }
            launchLockScreen(foregroundPackage)
        }
    }

    private fun launchLockScreen(lockedPackage: String) {
        startActivity(Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("locked_package", lockedPackage)
        })
    }

    private fun getTopPackage(): String? {
        val end = System.currentTimeMillis()
        val begin = end - 10_000L
        val events = usageStatsManager.queryEvents(begin, end)
        var lastResumedPackage: String? = null

        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPackage = event.packageName
            }
        }
        return lastResumedPackage
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "app_locker_service"
        private const val NOTIFICATION_ID = 101
        private const val POLL_INTERVAL_MS = 500L
    }
}
