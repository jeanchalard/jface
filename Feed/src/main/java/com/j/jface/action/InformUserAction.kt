package com.j.jface.action

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.support.design.widget.Snackbar
import android.view.View
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.SnackbarRegistry

class InformUserAction(val context : Context, val text : String, val actionMessage : String? = null, val callback : View.OnClickListener? = null, val pendingIntent : PendingIntent? = null) : () -> Unit
{
  companion object
  {
    const val CHANNEL_ID = "jorg"
    const val CHANNEL_NAME = "Jormungand"
    const val LAST_NOTIF_ID = "lastNotifId"
  }

  private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private fun getChannel() : NotificationChannel
  {
    val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
    if (null != existing) return existing
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
    // Configure the notification channel.
    channel.description = "Jormungand"
    channel.enableLights(true)
    channel.lightColor = context.getColor(R.color.jormungand_color)
    channel.enableVibration(false)
    notificationManager.createNotificationChannel(channel)
    return notificationManager.getNotificationChannel(CHANNEL_ID)
  }

  private fun postNotification() {
    val channel = getChannel()
    val notif = Notification.Builder(context, channel.id)
     .setSmallIcon(R.drawable.jormungand)
     .setColor(context.getColor(R.color.jormungand_color))
     .setTimeoutAfter(10_000)
    if (null != pendingIntent) notif.setContentIntent(pendingIntent)

    val index = text.indexOf('\n')
    if (index < 0)
      notif.setContentText(text)
    else
    {
      notif.setContentTitle(text.subSequence(0, index))
      notif.setStyle(Notification.BigTextStyle().bigText(text.subSequence(index, text.length)))
    }

    val persist: SharedPreferences = context.getSharedPreferences(Const.INTERNAL_PERSISTED_VALUES_FILES, Context.MODE_PRIVATE)
    val notifId = persist.getInt(LAST_NOTIF_ID, 1)
    persist.edit().putInt(LAST_NOTIF_ID, notifId + 1).apply()
    notificationManager.notify(notifId, notif.build())
  }

  override fun invoke()
  {
    val snackbarParent = SnackbarRegistry.getSnackbarParent()
    if (null == snackbarParent) { postNotification(); return }
    val sb = Snackbar.make(snackbarParent, text, Snackbar.LENGTH_LONG)
    if (null != actionMessage && null != callback)
      sb.setAction(actionMessage, callback)
    snackbarParent.post { sb.show() }
    pendingIntent?.send()
  }
}
