package com.timome.screenlock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.timome.screenlock.MainActivity
import com.timome.screenlock.R

object NotificationHelper {

    const val CHANNEL_ID_TIMER = "timer_channel"
    const val CHANNEL_ID_SERVICE = "service_channel"
    const val CHANNEL_ID_NOTIFICATION_MODE = "notification_mode_channel"

    const val NOTIFICATION_ID_TIMER = 100
    const val NOTIFICATION_ID_SERVICE = 101
    const val NOTIFICATION_ID_MODE = 102

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER,
                "定时器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "锁屏服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }

            val modeChannel = NotificationChannel(
                CHANNEL_ID_NOTIFICATION_MODE,
                "通知模式",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(timerChannel, serviceChannel, modeChannel)
            )
        }
    }

    fun createTimerNotification(
        context: Context,
        remainingTimeMs: Long
    ): Notification {
        val minutes = remainingTimeMs / 60000
        val seconds = (remainingTimeMs % 60000) / 1000
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, LockScreenService::class.java).apply {
            action = LockScreenService.ACTION_CANCEL_TIMER
        }
        val cancelPendingIntent = PendingIntent.getService(
            context, 1, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setContentTitle("屏幕锁定倒计时")
            .setContentText("剩余时间: $timeText")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .addAction(0, "取消", cancelPendingIntent)
            .build()
    }

    fun createNotificationModeNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(context, LockScreenService::class.java).apply {
            action = LockScreenService.ACTION_START
        }
        val startPendingIntent = PendingIntent.getService(
            context, 2, startIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_NOTIFICATION_MODE)
            .setContentTitle("屏幕锁 - 通知模式")
            .setContentText("点击启动按钮开始锁屏")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "启动", startPendingIntent)
            .build()
    }

    fun updateTimerNotification(context: Context, remainingTimeMs: Long) {
        val notification = createTimerNotification(context, remainingTimeMs)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_TIMER, notification)
    }

    fun cancelTimerNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID_TIMER)
    }

    fun cancelNotificationModeNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID_MODE)
    }
}
