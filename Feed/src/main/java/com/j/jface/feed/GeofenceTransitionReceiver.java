package com.j.jface.feed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.j.jface.R;
import com.j.jface.feed.actions.SetupGeofenceAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class GeofenceTransitionReceiver
{
  public static final String ACTION_MANUAL_START = "com.j.jface.feed.action.MANUAL_START";
  public static final String ACTION_GEOFENCE = "com.j.jface.feed.action.GEOFENCE";

  @NonNull private final Service service;
  @NonNull private final Client mClient;
  @NonNull private final FileOutputStream mLogFile;

  public GeofenceTransitionReceiver(@NonNull final Service s)
  {
    service = s;
    mClient = new Client(service);
    try { mLogFile = new FileOutputStream(new File(s.getExternalFilesDir(null), "log")); }
    catch (FileNotFoundException e) { throw new RuntimeException(e); }
    Logger.setLogger(this);
  }

  void onHandleIntent(@NonNull final Intent intent)
  {
    final String action = intent.getAction();
    if (ACTION_MANUAL_START.equals(action)) mClient.enqueue(new SetupGeofenceAction(getNotificationIntent()));
    if (ACTION_GEOFENCE.equals(action)) handleGeofenceTransitions(intent);
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
    if (event.hasError())
    {
      Logger.L("Geofencing error : " + event.getErrorCode());
      return;
    }

    final List<Geofence> fences = event.getTriggeringGeofences();
    final int transitionType = event.getGeofenceTransition();
    for (final Geofence fence : fences)
      handleGeofenceTransition(fence, transitionType);
  }

  private void handleGeofenceTransition(final Geofence fence, final int transitionType)
  {
    final Fences.Params params = Fences.paramsFromName(fence.getRequestId());
    final String message;
    if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType)
      message = "Entered " + fence.getRequestId();
    else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType)
      message = "Exited " + fence.getRequestId();
    else
      message = "Unknown event O.o";
    Logger.L(message);
  }


  public void Log(@NonNull final String s)
  {
    try
    {
      LogInternal(s);
    }
    catch (Exception e) {} // Ignore the fuck out of it
  }

  int notificationNumber = 1;
  private void LogInternal(@NonNull final String s) throws IOException
  {
    Log.e("JFACE", s);
    final Time t = new Time();
    t.setToNow();
    final String nowString = t.format3339(false);
    final String logString = nowString + " : " + s + "\n";
    mLogFile.write(logString.getBytes());
    final NotificationManager na = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
    na.notify(++notificationNumber, new Notification.Builder(service)
     .setSmallIcon(R.drawable.notif)
     .setContentTitle(s)
     .setContentText(nowString)
     .build());
  }
}
