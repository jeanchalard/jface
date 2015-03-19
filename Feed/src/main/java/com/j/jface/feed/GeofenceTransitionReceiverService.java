package com.j.jface.feed;

import android.app.IntentService;
import android.content.Intent;

public class GeofenceTransitionReceiverService extends IntentService
{
  private GeofenceTransitionReceiver r;
  public GeofenceTransitionReceiverService()
  {
    super("GeofenceTransitionReceiverService");
  }

  @Override public void onCreate()
  {
    super.onCreate();
    r = new GeofenceTransitionReceiver(this);
  }

  @Override protected void onHandleIntent(final Intent intent)
  {
    if (null == intent) return;
    r.onHandleIntent(intent);
  }
}
