package com.j.jface.org.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.j.jface.R
import com.j.jface.R.layout.todo
import com.j.jface.nextNotifId
import com.j.jface.notifManager
import com.j.jface.org.todo.TodoCore

class NotifEngine(val context : Context)
{
  enum class NotifType
  {
    SPLIT, // Split this TODO into multiple TODOs.
    FILLIN, // Please add missing info : deadline + hardness, estimated time, constraint
    SUGGESTION, // How about you do this now ? Constraints are fulfilled and it's not too hard
    REMINDER, // This deadline is very soon, make sure you don't forget
  }

  companion object
  {
    const val CHANNEL_ID = "jorg_todo"
    const val CHANNEL_NAME = "Jormungand : Todo"
    const val LAST_NOTIF_ID = "todo_lastId"

    internal fun getChannel(context : Context) : NotificationChannel
    {
      val notifManager = context.notifManager
      val existing = notifManager.getNotificationChannel(CHANNEL_ID)
      if (null != existing) return existing
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
      // Configure the notification channel.
      channel.description = "Jormungand : todo notifications"
      channel.enableLights(true)
      channel.lightColor = context.getColor(R.color.jormungand_color)
      channel.vibrationPattern = longArrayOf(0L, 30L, 30L, 60L) // First number is delay to vibrator on
      notifManager.createNotificationChannel(channel)
      return notifManager.getNotificationChannel(CHANNEL_ID)
    }
  }

  fun fillInNotification(todo : TodoCore) = notify(FillinNotification(context), todo)
  fun splitNotification(todo : TodoCore) = notify(SplitNotification(context), todo)
  fun suggestionNotification(todo : TodoCore) = notify(SuggestionNotification(context), todo)
  private fun notify(builder : NotificationBuilder, todo : TodoCore)
  {
    val id = context.nextNotifId(LAST_NOTIF_ID)
    val notification = builder.buildNotification(id, todo)
    context.notifManager.notify(id, notification)
  }
}
