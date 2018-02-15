package com.j.jface.feed;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.j.jface.Const;
import com.j.jface.action.GThread;
import com.j.jface.action.SetupGeofenceActionKt;

import java.util.List;

public class GeofenceTransitionReceiver
{
  public static final String ACTION_MANUAL_START = "com.j.jface.feed.action.MANUAL_START";
  public static final String ACTION_GEOFENCE = "com.j.jface.feed.action.GEOFENCE";

  @NonNull private final Service service;
  @NonNull private final GThread mGThread;

  public GeofenceTransitionReceiver(@NonNull final Service s)
  {
    service = s;
    mGThread = new GThread(service);
  }

  void onHandleIntent(@NonNull final Intent intent)
  {
    final String action = intent.getAction();
    if (Intent.ACTION_BOOT_COMPLETED.equals(action) || ACTION_MANUAL_START.equals(action))
    {
      mGThread.enqueue(SetupGeofenceActionKt.SetupGeofenceAction(mGThread, service, getNotificationIntent()));
    }
    if (ACTION_GEOFENCE.equals(action)) handleGeofenceTransitions(intent);
    FeedLoader.startAllLoads(mGThread);
  }

  private PendingIntent getNotificationIntent()
  {
    final Intent i = new Intent(service, GeofenceTransitionReceiverService.class);
    i.setAction(ACTION_GEOFENCE);
    return PendingIntent.getService(service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private void handleGeofenceTransitions(final Intent intent)
  {
    GeofencingEvent event = GeofencingEvent.fromIntent(intent);
    if (event.hasError()) return;

    final List<Geofence> fences = event.getTriggeringGeofences();
    final int transitionType = event.getGeofenceTransition();
    for (final Geofence fence : fences)
      handleGeofenceTransition(fence, transitionType);
  }

  private void handleGeofenceTransition(final Geofence fence, final int transitionType)
  {
    final Fences.Params params = Fences.paramsFromName(fence.getRequestId());
    if (null == params) return; // Unknown fence
    if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType)
      mGThread.putData(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, true);
    else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType)
      mGThread.putData(Const.LOCATION_PATH + "/" + params.name, Const.DATA_KEY_INSIDE, false);
  }
}
