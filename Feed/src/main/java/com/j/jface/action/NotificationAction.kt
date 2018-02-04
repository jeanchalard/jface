package com.j.jface.action

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.BitmapDrawable
import android.view.View
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.Snackbarable

class NotificationAction(val context : Context, val text : String) : Action()
{
  companion object
  {
    const val CHANNEL_ID = "jorg"
    const val CHANNEL_NAME = "Jormungand"
    const val LAST_NOTIF_ID = "lastNotifId"
  }

  val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private fun getChannel() : NotificationChannel
  {
    val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
    if (null != existing) return existing
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
    // Configure the notification channel.
    channel.description = "Jormungand";
    channel.enableLights(true)
    channel.lightColor = context.getColor(R.color.jormungand_color)
    channel.enableVibration(false)
    notificationManager.createNotificationChannel(channel)
    return notificationManager.getNotificationChannel(CHANNEL_ID)
  }

  fun postNotification() {
    val channel = getChannel()
    val notif = Notification.Builder(context, channel.id)
     .setLargeIcon((context.getDrawable(R.drawable.jormungand) as BitmapDrawable).bitmap)
     .setContentText(text)
     .setColor(context.getColor(R.color.jormungand_color))
    val persist: SharedPreferences = context.getSharedPreferences(Const.INTERNAL_PERSISTED_VALUES_FILES, Context.MODE_PRIVATE)
    val notifId = persist.getInt(LAST_NOTIF_ID, 1)
    persist.edit().putInt(LAST_NOTIF_ID, notifId + 1).apply()
    notificationManager.notify(notifId, notif.build())
  }

  override fun run() = postNotification()
}

class SnackbarAction(val dude : Snackbarable, val text : String, val actionMessage : String? = null, val callback : View.OnClickListener? = null) : Action()
{
  fun showSnackbar()
  {
    dude.showSnackbar(text, actionMessage, callback)
  }

  override fun run() = showSnackbar()
}

fun InformUserAction(context : Context, text : String) : Action
{
  return if (context is Snackbarable) SnackbarAction(context, text) else NotificationAction(context, text)
}
