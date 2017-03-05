package com.j.jface.client.action.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.j.jface.Const;
import com.j.jface.client.action.Action;
import com.j.jface.feed.Fences;

public class SetupGeofenceAction implements Action, ResultCallback<Status>
{
  @NonNull private final PendingIntent mIntent;
  public SetupGeofenceAction(@NonNull final PendingIntent intent)
  {
    mIntent = intent;
  }

  private static Geofence getGeofence(@NonNull final Fences.Params params)
  {
    return new Geofence.Builder()
     .setRequestId(params.name)
     .setCircularRegion(params.latitude, params.longitude, params.radius)
     .setExpirationDuration(Geofence.NEVER_EXPIRE)
     .setNotificationResponsiveness(2 * 60000) // 2 minutes
     .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
     .build();
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    final GeofencingRequest.Builder builder = new GeofencingRequest.Builder()
     .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT);
    for (final String fence : Const.ALL_FENCE_NAMES)
    {
      final Fences.Params params = Fences.paramsFromName(fence);
      if (null == params) throw new RuntimeException("Bug : Const.ALL_FENCE_NAMES contains a fence name not resolvable by Fences.paramsFromName");
      else builder.addGeofence(getGeofence(params));
    }
    final GeofencingRequest request = builder.build();

    final int hasPermission = ContextCompat.checkSelfPermission(client.getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
    if (PackageManager.PERMISSION_GRANTED == hasPermission)
      LocationServices.GeofencingApi.addGeofences(client, request, mIntent)
       .setResultCallback(this);
  }

  @Override public void onResult(@NonNull final Status status) {}
}
