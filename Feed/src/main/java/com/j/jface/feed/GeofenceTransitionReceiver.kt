package com.j.jface.feed

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.j.jface.Const
import com.j.jface.action.setupGeofence
import com.j.jface.feed.LocalLog.log
import com.j.jface.wear.Wear

class GeofenceTransitionReceiver() : BroadcastReceiver()
{
  companion object
  {
    const val ACTION_MANUAL_START = "com.j.jface.feed.action.MANUAL_START"
    const val ACTION_GEOFENCE = "com.j.jface.feed.action.GEOFENCE"
    private var serial = 0
  }
  private var wear : Wear? = null
  private fun wear(context : Context) : Wear {
    synchronized(this) {
      val w = wear ?: Wear(context)
      wear = w
      return w
    }
  }

  private fun getNotificationIntent(context : Context) : PendingIntent
  {
    val i = Intent(context, this::class.java)
    i.action = ACTION_GEOFENCE
    return PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
  }

  override fun onReceive(context : Context?, intent : Intent)
  {
    if (null == context) { Log.e("jface", "onReceive without context, srsly you guys"); return }
    Log.e("jface", "GeofenceTransitionReceiver : ${intent}")
    serial += 1
    val action = intent.action
    if (Intent.ACTION_BOOT_COMPLETED == action || ACTION_MANUAL_START == action)
      setupGeofence(context, getNotificationIntent(context)) // This is running in a background thread already. Don't bother running this on an executor.
    if (ACTION_GEOFENCE == action) handleGeofenceTransitions(context, intent)
    FeedLoader.startAllLoads(wear(context), "Intent ${action}")
  }

  private fun handleGeofenceTransitions(context : Context, intent : Intent)
  {
    val event = GeofencingEvent.fromIntent(intent)
    if (null == event || event.hasError()) return

    val fences = event.triggeringGeofences
    val transitionType = event.geofenceTransition
    if (null == fences) return
    for (fence in fences)
      handleGeofenceTransition(context, fence, transitionType)
  }

  private fun stypeFromType(i : Int) : String
  {
    return when (i)
    {
      Geofence.GEOFENCE_TRANSITION_ENTER -> "Enter fence"
      Geofence.GEOFENCE_TRANSITION_EXIT  -> "Exit fence"
      Geofence.GEOFENCE_TRANSITION_DWELL -> "Dwell fence"
      else                               -> "Fence wat ?"
    }
  }

  private fun handleGeofenceTransition(context : Context, fence : Geofence, transitionType : Int)
  {
    val params = Fences.paramsFromName(fence.requestId) ?: return // null means the fence is unknown, that's supposedly impossible
    log(context, "Geofence transition : ${stypeFromType(transitionType)}/${params.name}")
    if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType)
      wear(context).putDataLocally(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, true)
    else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType)
      wear(context).putDataLocally(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, false)
  }
}
