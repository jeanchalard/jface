package com.j.jface.feed

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.j.jface.Const
import com.j.jface.action.setupGeofence
import com.j.jface.wear.Wear

class GeofenceTransitionReceiver(private val service : Service)
{
  companion object
  {
    const val ACTION_MANUAL_START = "com.j.jface.feed.action.MANUAL_START"
    const val ACTION_GEOFENCE = "com.j.jface.feed.action.GEOFENCE"
    private var serial = 0
  }
  private val wear : Wear by lazy { Wear(service) }

  private val notificationIntent : PendingIntent
    get()
    {
      val i = Intent(service, GeofenceTransitionReceiverService::class.java)
      i.action = ACTION_GEOFENCE
      return PendingIntent.getService(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
    }

  fun onHandleIntent(intent : Intent)
  {
    serial += 1
    val action = intent.action
    if (Intent.ACTION_BOOT_COMPLETED == action || ACTION_MANUAL_START == action)
      setupGeofence(service, notificationIntent) // This is running in a background thread already. Don't bother running this on an executor.
    if (ACTION_GEOFENCE == action) handleGeofenceTransitions(intent)
    FeedLoader.startAllLoads(wear, "Intent ${action}")
  }

  private fun handleGeofenceTransitions(intent : Intent)
  {
    val event = GeofencingEvent.fromIntent(intent)
    if (event.hasError()) return

    val fences = event.triggeringGeofences
    val transitionType = event.geofenceTransition
    for (fence in fences)
      handleGeofenceTransition(fence, transitionType)
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

  private fun handleGeofenceTransition(fence : Geofence, transitionType : Int)
  {
    val params = Fences.paramsFromName(fence.requestId)
    if (null == params) return // Unknown fence
    if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType)
      wear.putDataLocally(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, true)
    else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType)
      wear.putDataLocally(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, false)
  }
}
