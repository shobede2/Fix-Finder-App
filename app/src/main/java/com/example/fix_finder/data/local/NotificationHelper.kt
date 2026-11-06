package com.example.fix_finder.data.local

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fix_finder.MainActivity
import com.example.fix_finder.R

object NotificationHelper {

    private const val CHANNEL_REPORTS_ID = "channel_fix_finder_reports"
    private const val CHANNEL_REPORTS_NAME = "Fix Finder Report Updates"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REPORTS_ID,
                CHANNEL_REPORTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for submitted service reports, status changes and offline sync"
            }
            val manager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        val appContext = context.applicationContext
        createNotificationChannels(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_REPORTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            manager.notify(notificationId, builder.build())
        } catch (_: Exception) {
            // Ignore notification posting exceptions
        }
    }

    fun showReportSubmittedNotification(context: Context, reportId: String, serviceName: String) {
        showNotification(
            context,
            "Service Request Submitted!",
            "Request #$reportId for $serviceName was successfully sent to the technician."
        )
    }

    fun showSyncCompletedNotification(context: Context, count: Int) {
        showNotification(
            context,
            "Offline Sync Completed ✓",
            "$count pending offline report${if (count > 1) "s were" else " was"} successfully synchronized to the cloud."
        )
    }
}
