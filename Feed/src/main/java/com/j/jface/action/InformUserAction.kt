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

class NotificationAction(private val context : Context, private val text : String, private val pendingIntent : PendingIntent? = null) : () -> Unit
{
  companion object
  {
    const val CHANNEL_ID = "jorg"
    const val CHANNEL_NAME = "Jormungand"
    const val LAST_NOTIF_ID = "lastNotifId"

    private fun getChannel(context : Context, notificationManager : NotificationManager) : NotificationChannel
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

    fun postNotification(context : Context, text : String, pendingIntent : PendingIntent?)
    {
      val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
      val channel = getChannel(context, notificationManager)
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
      val persist : SharedPreferences = context.getSharedPreferences(Const.INTERNAL_PERSISTED_VALUES_FILES, Context.MODE_PRIVATE)
      val notifId = persist.getInt(LAST_NOTIF_ID, 1)
      persist.edit().putInt(LAST_NOTIF_ID, notifId + 1).apply()
      notificationManager.notify(notifId, notif.build())
    }
  }

  override fun invoke() = postNotification(context, text, pendingIntent)
}

class SnackbarAction(private val text : String, private val actionMessage : String? = null, private val callback : View.OnClickListener? = null) : () -> Unit
{
  companion object
  {
    fun showSnackbar(text : String, actionMessage : String? = null, callback : View.OnClickListener? = null)
    {
      val snackbarParent = SnackbarRegistry.getSnackbarParent() ?: return
      val sb = Snackbar.make(snackbarParent, text, Snackbar.LENGTH_LONG)
      if (null != actionMessage && null != callback)
        sb.setAction(actionMessage, callback)
      snackbarParent.post { sb.show() }
    }
  }
  override fun invoke() = showSnackbar(text, actionMessage, callback)
}

class InformUserAction(private val context : Context, private val text : String, private val actionMessage : String? = null, private val callback : View.OnClickListener? = null, private val pendingIntent : PendingIntent? = null) : () -> Unit
{
  override fun invoke()
  {
    val snackbarParent = SnackbarRegistry.getSnackbarParent()
    if (null == snackbarParent) { NotificationAction.postNotification(context, text, pendingIntent); return }
    SnackbarAction.showSnackbar(text, actionMessage, callback)
    pendingIntent?.send()
  }
}
