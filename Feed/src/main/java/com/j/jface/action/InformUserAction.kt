package com.j.jface.action

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.bold
import com.google.android.material.snackbar.Snackbar
import com.j.jface.R
import com.j.jface.feed.LocalLog
import com.j.jface.feed.views.SnackbarRegistry
import com.j.jface.nextNotifId

const val CHANNEL_ID = "jorg_general"
const val CHANNEL_NAME = "Jormungand : General"
const val LAST_NOTIF_ID = "general_lastId"

class NotificationAction(private val context : Context, private val text : String, private val details : String? = null, private val pendingIntent : PendingIntent? = null) : () -> Unit
{
  companion object
  {
    private fun getChannel(context : Context, notificationManager : NotificationManager) : NotificationChannel
    {
      val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (null != existing) return existing
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
      // Configure the notification channel.
      channel.description = "Jormungand : debug and misc notifications"
      channel.enableLights(true)
      channel.lightColor = context.getColor(R.color.jormungand_color)
      channel.enableVibration(false)
      notificationManager.createNotificationChannel(channel)
      return notificationManager.getNotificationChannel(CHANNEL_ID)
    }

    fun postNotification(context : Context, text : String, details : String?, pendingIntent : PendingIntent?)
    {
      val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
      val channel = getChannel(context, notificationManager)
      val notif = Notification.Builder(context, channel.id)
       .setSmallIcon(R.drawable.jormungand)
       .setColor(context.getColor(R.color.jormungand_color))
       .setVisibility(Notification.VISIBILITY_PUBLIC)
       .setTimeoutAfter(10_000)
      if (null != pendingIntent) notif.setContentIntent(pendingIntent)

      if (null != details) {
        notif.setContentTitle(text)
        notif.setContentText(details)
      } else {
        val index = text.indexOf('\n')
        if (index < 0)
          notif.setContentText(text)
        else {
          notif.setContentTitle(text.subSequence(0, index))
          notif.style = Notification.BigTextStyle().bigText(text.subSequence(index, text.length))
        }
      }
      val notifId = context.nextNotifId(LAST_NOTIF_ID)
      notificationManager.notify(notifId, notif.build())
    }
  }

  override fun invoke() = postNotification(context, text, details, pendingIntent)
}

class SnackbarAction(private val text : String, private val details : String? = null, private val actionMessage : String? = null, private val callback : View.OnClickListener? = null) : () -> Unit
{
  companion object
  {
    fun showSnackbar(text : String, details : String? = null, actionMessage : String? = null, callback : View.OnClickListener? = null)
    {
      val message = if (null != details) SpannableStringBuilder().bold { append(text) }.append("\n").append(details) else text
      val snackbarParent = SnackbarRegistry.getSnackbarParent() ?: return
      val sb = Snackbar.make(snackbarParent, message, Snackbar.LENGTH_LONG)
      if (null != actionMessage && null != callback)
        sb.setAction(actionMessage, callback)
      snackbarParent.post { sb.show() }
    }
  }
  override fun invoke() = showSnackbar(text, details, actionMessage, callback)
}

class InformUserAction(private val context : Context, private val text : String, private val details : String? = null, private val actionMessage : String? = null, private val callback : View.OnClickListener? = null, private val pendingIntent : PendingIntent? = null) : () -> Unit
{
  override fun invoke()
  {
    LocalLog.log(context, if (null == details) text else "${text}\n${details}")
    val snackbarParent = SnackbarRegistry.getSnackbarParent()
    if (null == snackbarParent) { NotificationAction.postNotification(context, text, details, pendingIntent); return }
    SnackbarAction.showSnackbar(text, details, actionMessage, callback)
    pendingIntent?.send()
  }
}
