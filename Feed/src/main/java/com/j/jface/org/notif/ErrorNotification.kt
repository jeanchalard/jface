package com.j.jface.org.notif

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.j.jface.R

fun errorNotification(text : String, context : Context, notificationManager : NotificationManager) : Notification
{
  return Notification.Builder(context, NotifEngine.getChannel(context, notificationManager).id)
   .setSmallIcon(R.drawable.ic_error)
   .setContentText(text)
   .build()
}
