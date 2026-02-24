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

class LockService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 500L // Polling every 500ms
    private val notificationId = 101
    private val channelId = "LockerServiceChannel"

    private lateinit var usageStatsManager: UsageStatsManager
    private var lastObservedPackage: String = ""

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        SecurityUtil.init(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        startForeground(notificationId, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(monitorRunnable)
        if (SecurityUtil.isLockEnabled()) {
            handler.postDelayed(monitorRunnable, checkInterval)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }

    private fun checkCurrentApp() {
        val topPackage = getTopApp() ?: return

        // If the top app changes and it's not the locker itself
        if (topPackage != lastObservedPackage && topPackage != packageName) {
            // We moved away from the temporarily unlocked app
            if (SecurityUtil.isTemporarilyUnlocked(lastObservedPackage)) {
                SecurityUtil.clearTemporarilyUnlocked()
            }
        }

        lastObservedPackage = topPackage

        // Exclude our own app from being locked by the service loop
        if (topPackage == packageName) return

        if (SecurityUtil.isAppLocked(topPackage) && !SecurityUtil.isTemporarilyUnlocked(topPackage)) {
            showLockScreen(topPackage)
        }
    }

    private fun showLockScreen(lockedPackage: String) {
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("locked_package", lockedPackage)
        }
        startActivity(lockIntent)
    }

    private fun getTopApp(): String? {
        var topPackage: String? = null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 // last minute

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var lastEvent: UsageEvents.Event? = null

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                lastEvent = event
            }
        }

        if (lastEvent != null && lastEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            topPackage = lastEvent.packageName
        }

        return topPackage
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "App Locker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Locker is Running")
            .setContentText("Monitoring secured apps")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()
    }
}
