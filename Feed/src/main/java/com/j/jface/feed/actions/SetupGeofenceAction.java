package com.j.jface.feed.actions;

import android.app.PendingIntent;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.j.jface.Const;
import com.j.jface.feed.Fences;
import com.j.jface.feed.Logger;

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
      builder.addGeofence(getGeofence(Fences.paramsFromName(fence)));
    final GeofencingRequest request = builder.build();

    LocationServices.GeofencingApi.addGeofences(client, request, mIntent)
     .setResultCallback(this);
  }

  @Override
  public void onResult(final Status status)
  {
    Logger.L("Added geofences : " + status);
  }
}
