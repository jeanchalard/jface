package com.j.jface.org.notif

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.org.AutomaticEditorProcessor
import com.j.jface.org.JOrg
import com.j.jface.org.editor.TodoEditor
import com.j.jface.org.todo.TodoCore

/**
 * A notification suggesting the user split a TODO into multiple subitems.
 */
class SplitNotification(val context : Context)
{
  private fun TodoCore.timeAgo() : String
  {
    val HOUR = 60L * 60 * 1_000
    val DAY = 24 * HOUR
    val MONTH = 30 * DAY // 30 days is a month, sure
    val diff = System.currentTimeMillis() - this.lastUpdateTime
    if (diff < 0) return "in the future"
    return if (diff > 2 * MONTH)
      "${diff / MONTH} months ago"
    else if (diff > MONTH)
      "last month"
    else if (diff > 2 * DAY)
      "${diff / DAY} days ago"
    else if (diff > DAY)
      "yesterday"
    else if (diff > 2 * HOUR)
      "${diff / HOUR} hours ago"
    else
      "in the last two hours"
  }

  private fun buildSplitNotificationAction(todo : TodoCore) : Notification.Action
  {
    val intent = Intent(context, AutomaticEditorProcessor.Receiver::class.java)
    intent.putExtra(Const.EXTRA_TODO_ID, todo.id)
    val pendingIntent = PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
    return Notification.Action.Builder(null, "Subitems", pendingIntent)
     .addRemoteInput(RemoteInput.Builder(Const.EXTRA_TODO_SUBITEMS)
      .setAllowFreeFormInput(true)
      .build())
     .build()
  }

  internal fun buildSplitNotification(todo : TodoCore, notificationManager : NotificationManager) : Notification
  {
    val intent = Intent(context, JOrg.activityClass())
    intent.putExtra(Const.EXTRA_TODO_ID, todo.id)
    val pendingIntent = PendingIntent.getActivity(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT)

    val title = "Split todo : " + todo.text
    val description = "This todo has last been updated " + todo.timeAgo() + " ; do something and split it up"
    return Notification.Builder(context, NotifEngine.getChannel(context, notificationManager).id)
     .setShowWhen(true)
     .setWhen(System.currentTimeMillis())
     .setSmallIcon(R.drawable.jormungand)
     .setColor(context.getColor(R.color.jormungand_color))
     .setContentIntent(pendingIntent)
     .setContentTitle(title)
     .setContentText(description)
     .setStyle(Notification.BigTextStyle().bigText(description))
     .setAutoCancel(true)
     .setOnlyAlertOnce(true)
     .setCategory(Notification.CATEGORY_REMINDER)
     .setVisibility(Notification.VISIBILITY_SECRET)
     .addAction(buildSplitNotificationAction(todo))
     //.setContentIntent()
     //.setDeleteIntent() // when dismissed
     //.setCustomRemoveViews() // bazooka
     //.extend(WearableExtender) // <a href="{@docRoot}wear/notifications/creating.html">Creating Notifications for Android Wear</a>
     .build()
  }

}