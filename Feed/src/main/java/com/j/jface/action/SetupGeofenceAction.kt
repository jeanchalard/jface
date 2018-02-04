package com.j.jface.action

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.j.jface.Const
import com.j.jface.feed.Fences

class SetupGeofenceAction(val context : Context, val intent : PendingIntent) : Action()
{
  private fun getGeofence(params : Fences.Params) : Geofence
  {
    return Geofence.Builder()
     .setRequestId(params.name)
     .setCircularRegion(params.latitude, params.longitude, params.radius)
     .setExpirationDuration(Geofence.NEVER_EXPIRE)
     .setNotificationResponsiveness(2 * 60000) // 2 minutes
     .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
     .build()
  }

  override fun run()
  {
    val client = LocationServices.getGeofencingClient(context)

    val builder = GeofencingRequest.Builder()
     .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
    for (fence in Const.ALL_FENCE_NAMES)
    {
      val params = Fences.paramsFromName(fence)
      if (null == params)
        throw RuntimeException("Bug : Const.ALL_FENCE_NAMES contains a fence name not resolvable by Fences.paramsFromName")
      else
        builder.addGeofence(getGeofence(params))
    }
    val request = builder.build()

    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    if (PackageManager.PERMISSION_GRANTED == hasPermission)
      client.addGeofences(request, intent).addOnCompleteListener { task -> if (task.isSuccessful) InformUserAction(context, "Geofences added.").enqueue() else InformUserAction(context, "Geofences can't be added :\n" + task.exception).enqueue() }
    else
      InformUserAction(context, "Can't add geofences for lack of location permission.").enqueue()
  }
}
