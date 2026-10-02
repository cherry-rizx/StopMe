package com.hanyz.stopme.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hanyz.stopme.MainActivity
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.alarm.AlarmActivity

object NotificationHelper {

    const val TRACKING_CHANNEL_ID = "stopme_tracking_channel"
    const val ALARM_CHANNEL_ID = "stopme_alarm_channel"
    const val REMINDER_CHANNEL_ID = "stopme_reminder_channel"
    const val REMINDER_NOTIF_ID = 1004
    const val TRACKING_NOTIF_ID = 1001
    const val ALARM_NOTIF_ID = 1002
    const val FULL_ALARM_NOTIF_ID = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel untuk pemantauan latar depan (Foreground Service)
            val trackingChannel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }

            // Channel prioritas tinggi untuk alarm tujuan
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                context.getString(R.string.notif_alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_alarm_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSoundUri, audioAttributes)
            }

            // Channel pengingat "halte berikutnya tujuanmu": muncul di atas layar, tanpa suara
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Pengingat Halte Berikutnya",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan saat halte berikutnya adalah tujuanmu"
                setSound(null, null)
                enableVibration(false) // getar singkat dijalankan oleh service
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    // Pengingat ringan: halte berikutnya adalah tujuan (bukan alarm penuh)
    fun buildNextStopReminder(context: Context, destinationName: String): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            4,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_stopme)
            .setContentTitle("Halte berikutnya: $destinationName")
            .setContentText("Bersiaplah turun. Alarm tetap bunyi sesuai pengaturanmu.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
    }

    // Notifikasi foreground persisten selama pemantauan
    fun buildTrackingNotification(
        context: Context,
        destinationName: String,
        distanceStr: String,
        etaStr: String
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(R.string.notif_content_format, destinationName, distanceStr, etaStr)

        return NotificationCompat.Builder(context, TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_stopme)
            .setContentTitle("StopMe: Menuju $destinationName")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    // Notifikasi heads-up alarm prioritas tinggi dengan full-screen intent
    fun buildHeadsUpAlarmNotification(
        context: Context,
        destinationName: String,
        distanceStr: String,
        alarmTitle: String? = null
    ): Notification {
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            2,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tombol di notifikasi mematikan alarm (perjalanan lanjut jika masih ada alarm berikutnya)
        val stopIntent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_DISMISS_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_stopme)
            .setContentTitle(alarmTitle ?: context.getString(R.string.notif_heads_up_title))
            .setContentText(context.getString(R.string.notif_heads_up_text, destinationName, distanceStr))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.turn_off_alarm),
                stopPendingIntent
            )
            .build()
    }
}
