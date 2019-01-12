package com.j.jface.org.notif

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.org.AutomaticEditorProcessor
import com.j.jface.org.JOrg
import com.j.jface.org.todo.TodoCore

class SuggestionNotification(val context : Context)
{
  private fun buildSuggestionNotificationActions(existingIntent : Intent) : Notification.Action
  {
    val intent = Intent(existingIntent).setClass(context, AutomaticEditorProcessor.Receiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
    return Notification.Action.Builder(null, "Done", pendingIntent).build()
  }

  fun buildSuggestionNotification(id : Int, todo : TodoCore) : Notification
  {
    val title = "Got ${if (todo.estimatedMinutes > 5) "10" else "5"} min ?"
    val description = "Do this : " + todo.text
    val intent = Intent(context, JOrg.activityClass())
     .putExtra(Const.EXTRA_TODO_ID, todo.id)
     .putExtra(Const.EXTRA_NOTIF_ID, id)
     .putExtra(Const.EXTRA_NOTIF_TYPE, Const.NOTIFICATION_TYPE_SUGGESTION)
    val pendingIntent = PendingIntent.getActivity(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
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
      // setVisibility(Notification.VISIBILITY_SECRET) // Broken in the latest custom build apparently
      addAction(buildSuggestionNotificationActions(intent))
    }.build()
  }

  fun buildAckOrCancelNotification(id : Int, todo : TodoCore) : Notification
  {
    val title = "Marked done, tap to undo"
    val description = todo.text
    val intent = Intent(context, AutomaticEditorProcessor.Receiver::class.java)
     .putExtra(Const.EXTRA_TODO_ID, todo.id)
     .putExtra(Const.EXTRA_NOTIF_ID, id)
     .putExtra(Const.EXTRA_NOTIF_TYPE, Const.NOTIFICATION_TYPE_CANCEL_DONE)
    val pendingIntent = PendingIntent.getBroadcast(context, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT)
    return Notification.Builder(context, NotifEngine.getChannel(context).id).apply {
      setSmallIcon(R.drawable.jormungand)
      setColor(context.getColor(R.color.jormungand_color))
      setContentIntent(pendingIntent)
      setContentTitle(title)
      setContentText(description)
      setTimeoutAfter(8000) // 8 seconds ought to be enough for anybody
      setAutoCancel(true)
      setOnlyAlertOnce(true)
      // setVisibility(Notification.VISIBILITY_SECRET) // Broken in the latest custom build apparently
    }.build()
  }
}
