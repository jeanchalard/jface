package com.j.jface.org.notif

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.lifecycle.AuthTrampolineJOrgBoot
import com.j.jface.lifecycle.JOrgBoot
import com.j.jface.nextNotifId
import com.j.jface.org.todo.TodoCore

const val CHANNEL_ID = "jorg_todo"
const val CHANNEL_NAME = "Jormungand : Todo"
const val LAST_NOTIF_ID = "todo_lastId"

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
    private fun getChannel(context : Context, notificationManager : NotificationManager) : NotificationChannel
    {
      val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (null != existing) return existing
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
      // Configure the notification channel.
      channel.description = "Jormungand : todo notifications"
      channel.enableLights(true)
      channel.lightColor = context.getColor(R.color.jormungand_color)
      channel.vibrationPattern = longArrayOf(0L, 30L, 30L, 60L) // First number is delay to vibrator on
      notificationManager.createNotificationChannel(channel)
      return notificationManager.getNotificationChannel(CHANNEL_ID)
    }
  }

  private fun PendingIntentForActivity(k : Class<*>, resultCode : Int) : PendingIntent
   = PendingIntent.getActivity(context, resultCode, Intent(context, k), PendingIntent.FLAG_ONE_SHOT)

  private fun buildSplitNotificationAction() : Notification.Action
  {
    val icon = Icon.createWithResource(context, R.drawable.ic_clear_white_24dp)
    val intent = PendingIntentForActivity(AuthTrampolineJOrgBoot::class.java, Const.NOTIFICATION_RESULT_CODE)
    return Notification.Action.Builder(icon, "Components", intent)
     .addRemoteInput(RemoteInput.Builder(Const.EXTRA_SPLIT_TODOS)
      .setAllowFreeFormInput(true)
      .build())
     .build()
  }

  private fun buildSplitNotification(todo : TodoCore, notificationManager : NotificationManager) : Notification
  {
    return Notification.Builder(context, getChannel(context, notificationManager).id)
     .setShowWhen(true)
     .setWhen(System.currentTimeMillis())
     .setSmallIcon(R.drawable.jormungand)
     .setColor(context.getColor(R.color.jormungand_color))
     .setContentTitle("Split todo : " + todo.text)
     .setOnlyAlertOnce(true)
     .setCategory(Notification.CATEGORY_REMINDER)
     .setVisibility(Notification.VISIBILITY_SECRET)
     .addAction(buildSplitNotificationAction())
    //.setContentIntent()
    //.setDeleteIntent() // when dismissed
    //.setCustomRemoveViews() // bazooka
    //.extend(WearableExtender) // <a href="{@docRoot}wear/notifications/creating.html">Creating Notifications for Android Wear</a>
     .build()
  }

  fun splitNotification(todo : TodoCore)
  {
    val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
    val notification = buildSplitNotification(todo, notificationManager)
    val id = context.nextNotifId(LAST_NOTIF_ID)
    notificationManager.notify(id, notification)
  }
}
