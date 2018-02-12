package com.j.jface.action

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.BitmapDrawable
import android.support.design.widget.Snackbar
import android.view.View
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.views.Snackbarable

class InformUserAction(val context : Context, val text : String, val actionMessage : String? = null, val callback : View.OnClickListener? = null) : Action
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
    channel.description = "Jormungand"
    channel.enableLights(true)
    channel.lightColor = context.getColor(R.color.jormungand_color)
    channel.enableVibration(false)
    notificationManager.createNotificationChannel(channel)
    return notificationManager.getNotificationChannel(CHANNEL_ID)
  }

  fun postNotification() {
    val channel = getChannel()
    val notif = Notification.Builder(context, channel.id)
     .setSmallIcon(R.drawable.jormungand)
     .setLargeIcon((context.getDrawable(R.drawable.jormungand) as BitmapDrawable).bitmap)
     .setContentText(text)
     .setColor(context.getColor(R.color.jormungand_color))
    val persist: SharedPreferences = context.getSharedPreferences(Const.INTERNAL_PERSISTED_VALUES_FILES, Context.MODE_PRIVATE)
    val notifId = persist.getInt(LAST_NOTIF_ID, 1)
    persist.edit().putInt(LAST_NOTIF_ID, notifId + 1).apply()
    notificationManager.notify(notifId, notif.build())
  }

  override fun run()
  {
    if (context !is Snackbarable) { postNotification(); return }
    val snackbarParent = context.snackbarParent
    if (null == snackbarParent) { postNotification(); return }
    val sb = Snackbar.make(snackbarParent, text, Snackbar.LENGTH_SHORT)
    if (null != actionMessage && null != callback)
      sb.setAction(actionMessage, callback)
    snackbarParent.post { sb.show() }
  }
}
