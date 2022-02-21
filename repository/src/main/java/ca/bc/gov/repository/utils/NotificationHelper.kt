package ca.bc.gov.repository.utils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ca.bc.gov.common.R
import javax.inject.Inject

/**
 * @Author: Created by Rashmi Bambhania on 15,February,2022
 */

const val CHANNEL_ID = "my_health_channel_id"
const val BACKGROUND_WORK_NOTIFICATION_ID = 1

class NotificationHelper @Inject constructor(private val application: Application) {

    private lateinit var notificationBuilder: NotificationCompat.Builder

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            CHANNEL_ID,
            application.getString(R.string.notification_channel_name),
            importance
        ).apply {
            description = application.getString(R.string.notification_channel_work)
        }

        val notificationManager: NotificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(title: String, message: String) {
        notificationBuilder = NotificationCompat.Builder(application, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false)
        // .setOngoing(true)

        with(NotificationManagerCompat.from(application)) {
            notify(BACKGROUND_WORK_NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    fun updateNotification(title: String, message: String) {
        notificationBuilder
            .setContentTitle(title)
            .setContentText(message)

        with(NotificationManagerCompat.from(application)) {
            notify(BACKGROUND_WORK_NOTIFICATION_ID, notificationBuilder.build())
        }
    }
}