package com.example.mcmovilcare.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificacionHelper {
    private const val CHANNEL_ID = "mcmovilcare_channel_silent"

    fun enviarNotificacion(context: Context, titulo: String, mensaje: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Elimina los canales anteriores para que Android no conserve el sonido en caché
            notificationManager.deleteNotificationChannel("mcmovilcare_channel_light")
            notificationManager.deleteNotificationChannel("mcmovilcare_channel_dark")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones MCmovilCare",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSound(null)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}