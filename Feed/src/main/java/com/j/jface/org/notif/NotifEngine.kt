package com.j.jface.org.notif

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.j.jface.R
import com.j.jface.nextNotifId
import com.j.jface.notifManager
import com.j.jface.org.todo.TodoCore

@SuppressLint("NewApi")
class NotifEngine(val context : Context)
{
  enum class NotifType(override val weight : Int) : WeightedChoice
  {
    SPLIT(150), // Split this TODO into multiple TODOs.
    FILLIN(300), // Please add missing info : deadline + hardness, estimated time, constraint
    SUGGESTION(100), // How about you do this now ? Constraints are fulfilled and it's not too hard
    REMINDER(0), // This deadline is very soon, make sure you don't forget
  }

  companion object
  {
    private const val CHANNEL_ID = "jorg_todo"
    private const val CHANNEL_NAME = "Jormungand : Todo"
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

  internal fun fillInNotification(todo : TodoCore) = notify(FillinNotification(context), todo)
  internal fun splitNotification(todo : TodoCore) = notify(SplitNotification(context), todo)
  internal fun suggestionNotification(todo : TodoCore) = notify(SuggestionNotification(context), todo)
  private fun notify(builder : NotificationBuilder, todo : TodoCore)
  {
    val id = context.nextNotifId(LAST_NOTIF_ID)
    val notification = builder.buildNotification(id, todo)
    context.notifManager.notify(id, notification)
  }

  fun chooseNotification()
  {
    chooseWeighted(enumValues<NotifType>())
  }
}
