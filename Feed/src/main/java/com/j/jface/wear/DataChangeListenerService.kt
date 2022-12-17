package com.j.jface.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.feed.JFaceDataFeed
import com.j.jface.feed.OPEN_FRAGMENT_EXTRA
import com.j.jface.notifManager
import com.j.jface.org.AutomaticEditorProcessor
import com.j.jface.org.notif.NotifEngine

private const val CHANNEL_ID = "jface_checkpoints"
private const val CHANNEL_NAME = "JFace : checkpoints"
private const val NOTIF_ID = 1
private const val DELETE_CHECKPOINTS_ACTION = "delete checkpoints"

class DataChangeListenerService : WearableListenerService()
{
  override fun onDataChanged(eb : DataEventBuffer)
  {
    if (null == eb)
    {
      Log.e("JFace", "DataChangeListenerService : onDataChanged with null argument")
      return
    }
    eb.forEach {
      if (DataEvent.TYPE_CHANGED != it.type) return // Don't handle deletions in this version
      val path = it.dataItem.uri.path
      if (null == path)
      {
        Log.e("JFace", "Updated data path is null ?!")
        return
      }
      if (!path.startsWith(Const.DATA_PATH)) return // Only interested in data path stuff
      when (val key = path.substring(Const.DATA_PATH.length + 1)) // +1 for the "/" separator
      {
        Const.DATA_KEY_CHECKPOINTS -> DataMapItem.fromDataItem(it.dataItem).dataMap.get<String>(key).let { maybeCheckpoints ->
          (maybeCheckpoints ?: "").let { checkpoints ->
            notify(checkpoints)
            Wear(this).putDataToCloudOnly(path, key, checkpoints)
          }
        }
      }
    }
  }

  override fun onStartCommand(intent : Intent?, flags : Int, startId : Int) : Int {
    if (null != intent)
    {
      when (intent.action)
      {
        DELETE_CHECKPOINTS_ACTION -> {
          notify("")
          Wear(this).deleteData("${Const.DATA_PATH}/${Const.DATA_KEY_CHECKPOINTS}")
        }
      }
    }
    return super.onStartCommand(intent, flags, startId)
  }

  private fun getChannel() : NotificationChannel
  {
    val notifManager = notifManager
    val existing = notifManager.getNotificationChannel(CHANNEL_ID)
    if (null != existing) return existing
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    // Configure the notification channel.
    channel.description = "JFace : checkpoints"
    channel.enableLights(false)
    channel.lightColor = getColor(R.color.jormungand_color)
    channel.enableVibration(false)
    notifManager.createNotificationChannel(channel)
    return notifManager.getNotificationChannel(CHANNEL_ID)
  }

  private fun buildDeleteButton() : Notification.Action
  {
    val intent = Intent().setClass(this, DataChangeListenerService::class.java).apply {
      action = DELETE_CHECKPOINTS_ACTION
    }
    val pendingIntent = PendingIntent.getService(this, Const.CHECKPOINTS_CLEAR, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
    return Notification.Action.Builder(null, "Delete", pendingIntent).build()
  }

  private fun notify(checkpoints : String)
  {
    if (checkpoints.isEmpty())
    {
      notifManager.cancel(NOTIF_ID)
      return
    }
    val title = "JFace"
    val intent = Intent(this, JFaceDataFeed.activityClass())
    intent.putExtra(OPEN_FRAGMENT_EXTRA, "Messages")
    val pendingIntent = PendingIntent.getActivity(this, Const.NOTIFICATION_RESULT_CODE, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
    val notification = Notification.Builder(this, getChannel().id).apply {
      setShowWhen(true)
      setWhen(System.currentTimeMillis())
      setSmallIcon(R.drawable.ic_launcher)
      setColor(getColor(R.color.jormungand_color))
      setContentIntent(pendingIntent)
      setContentTitle(title)
      setContentText(checkpoints)
      setStyle(Notification.BigTextStyle().bigText(checkpoints))
      setAutoCancel(true)
      setOnlyAlertOnce(true)
      setCategory(Notification.CATEGORY_REMINDER)
      setLocalOnly(true) // Don't show this notification on the watch, it comes from there
      addAction(buildDeleteButton())
      // setVisibility(Notification.VISIBILITY_SECRET) // Broken in the latest custom build apparently
    }.build()
    notifManager.notify(NOTIF_ID, notification)
  }
}
