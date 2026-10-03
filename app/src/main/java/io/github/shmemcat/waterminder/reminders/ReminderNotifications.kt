package io.github.shmemcat.waterminder.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.shmemcat.waterminder.MainActivity
import io.github.shmemcat.waterminder.R
import java.time.Instant
import java.time.ZoneId

object ReminderNotifications {
    const val CHANNEL_ID = "water_reminders"
    private const val ID = 8
    private var testQueued = false

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Little water nudges", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "A gentle reminder to take a sip of water"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 100, 90, 100)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(context: Context, settings: ReminderSettings): Boolean {
        if (!canNotify(context) || ReminderPolicy.isQuiet(Instant.now(), settings, ZoneId.systemDefault())) return false
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val drank = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java).setAction(MainActivity.DRANK_WATER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF728A72.toInt())
            .setContentTitle("A little water break?")
            .setContentText("Take a sip. Your little plant is rooting for you.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(0, "I drank water", drank)
            .setAutoCancel(true)
            .build()
        // Permission can change between checking and posting.
        try { NotificationManagerCompat.from(context).notify(ID, notification) }
        catch (_: SecurityException) { return false }
        if (settings.wakeScreen) wakeBriefly(context)
        return true
    }

    fun dismiss(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    fun queueTest(context: Context): Boolean {
        val settings = SettingsStore(context).read()
        if (!canNotify(context) || ReminderPolicy.isQuiet(Instant.now(), settings, ZoneId.systemDefault())) return false
        if (testQueued) return true
        // A short, explicitly requested test gives the user time to lock the phone.
        // Only this test holds the CPU awake; recurring reminders use AlarmManager.
        val lock = context.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "waterminder:test")
        lock.acquire(15_000L)
        testQueued = true
        Handler(Looper.getMainLooper()).postDelayed({
            try { show(context, SettingsStore(context).read()) }
            finally { testQueued = false; if (lock.isHeld) lock.release() }
        }, 10_000L)
        return true
    }

    @Suppress("DEPRECATION")
    private fun wakeBriefly(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Respect Do Not Disturb even on devices that let a wake lock turn on the screen.
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        val power = context.getSystemService(PowerManager::class.java)
        if (power.isInteractive) return
        try {
            // There is no modern cross-device API for waking the screen for an ordinary
            // notification. This optional legacy path is best-effort, with a normal
            // lock-screen notification as fallback. Never launch an activity from here.
            val lock = power.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "waterminder:notification",
            )
            lock.acquire(3_000L)
            Handler(Looper.getMainLooper()).postDelayed({ if (lock.isHeld) lock.release() }, 2_500L)
        } catch (_: SecurityException) {
            // Some Android versions/OEMs disallow screen wake. Notification is already posted.
        }
    }
}
