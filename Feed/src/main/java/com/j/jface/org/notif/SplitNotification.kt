package com.j.jface.org.notif

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.org.AutomaticEditorProcessor
import com.j.jface.org.JOrg
import com.j.jface.org.todo.TodoCore

const val HOUR = 60L * 60 * 1_000
const val DAY = 24 * HOUR
const val MONTH = 30 * DAY // 30 days is a month, sure

/**
 * A notification suggesting the user split a TODO into multiple subitems.
 */
class SplitNotification(val context : Context) : NotificationBuilder
{
  private fun TodoCore.timeAgo() : String
  {
    val diff = System.currentTimeMillis() - this.lastUpdateTime
    if (diff < 0) return "in the future"
    return when
    {
      diff > 2 * MONTH -> "${diff / MONTH} months ago"
      diff > MONTH     -> "last month"
      diff > 2 * DAY   -> "${diff / DAY} days ago"
      diff > DAY       -> "yesterday"
      diff > 2 * HOUR  -> "${diff / HOUR} hours ago"
      else             -> "in the last two hours"
    }
  }

  private fun buildSplitNotificationAction(existingIntent : Intent) : Notification.Action
  {
    val intent = Intent(existingIntent).setClass(context, AutomaticEditorProcessor.Receiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
    return Notification.Action.Builder(null, "Subitems", pendingIntent)
     .addRemoteInput(RemoteInput.Builder(Const.EXTRA_TODO_SUBITEMS)
      .setAllowFreeFormInput(true)
      .build())
     .build()
  }

  override fun buildNotification(id : Int, todo : TodoCore) : Notification
  {
    val intent = Intent(context, JOrg.activityClass())
     .putExtra(Const.EXTRA_TODO_ID, todo.id)
     .putExtra(Const.EXTRA_NOTIF_ID, id)
     .putExtra(Const.EXTRA_NOTIF_TYPE, Const.NOTIFICATION_TYPE_SPLIT)
    val pendingIntent = PendingIntent.getActivity(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)

    val title = "Split todo : " + todo.text
    val description = "This todo has last been updated " + todo.timeAgo() + " ; do something and split it up"
    return Notification.Builder(context, NotifEngine.getChannel(context).id).apply {
      setShowWhen(true)
      setWhen(System.currentTimeMillis())
      setSmallIcon(R.drawable.jormungand)
      setColor(context.getColor(R.color.jormungand_color))
      setContentIntent(pendingIntent)
      setContentTitle(title)
      setContentText(description)
      setStyle(Notification.BigTextStyle().bigText(description))
      setAutoCancel(true)
      setOnlyAlertOnce(true)
      setCategory(Notification.CATEGORY_REMINDER)
      setVisibility(Notification.VISIBILITY_SECRET)
      addAction(buildSplitNotificationAction(intent))
      // setContentIntent()
      // setDeleteIntent() // when dismissed
      // setCustomRemoveViews() // bazooka
      // extend(WearableExtender) // <a href="{@docRoot}wear/notifications/creating.html">Creating Notifications for Android Wear</a>
    }.build()
  }

  fun buildAckNotification(parent : TodoCore, children : ArrayList<TodoCore>) : Notification
  {
    val style = Notification.BigTextStyle()
     .setBigContentTitle(parent.text)
     .bigText("　├ " + children.map(TodoCore::text).joinToString("\n　├ ").replace("├([^├]*)\\Z".toRegex(), "└$1"))
    return Notification.Builder(context, NotifEngine.getChannel(context).id)
     .setSmallIcon(R.drawable.ic_done)
     .setStyle(style)
     .setTimeoutAfter(10_000)
     .build()
 }
}
