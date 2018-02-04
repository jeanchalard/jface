package com.j.jface.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color

class NotificationAction
{
  fun createChannel(context : Context)
  {
    val notMng = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel("jorg", "Jormungand", NotificationManager.IMPORTANCE_HIGH)
    // Configure the notification channel.
    channel.setDescription("Jormungand")
    channel.enableLights(true)
    channel.setLightColor(Color.RED)
    channel.enableVibration(false)
    notMng.createNotificationChannel(channel)
  }
}
