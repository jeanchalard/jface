package com.j.jface.feed;

import android.app.IntentService;
import android.content.Intent;

import com.j.jface.Util;

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
    // The system has an ugly tendency to send events to this service just one run loop apart.
    // While this works correctly it's pretty wasteful, as it's tearing down this service after
    // each intent because it has no more work to do, only to recreate it a split second later
    // to handle the next intent. It creates multiple GeofenceTransitionReceiver objects, which
    // in turn create Client objects... It's design to work even so, but it's ugly and wasteful.
    // So here sleep 600ms to give a chance for other events to arrive before this service is
    // stopped. It's not very pretty but it's the most efficient way and the simplest to do it
    // there is no explicit way to ask for no kill for a few seconds, and any other mechanism
    // to avoid recreating the stuff under this service would be clearly more complex and
    // dangerous than this one line, and would not avoid the service recreation.
    // This mechanism is not foolproof but it need not be, it's just here to try and alleviate
    // some of the waste. Also it's a bit detrimental in that when receiving multiple intents
    // at the same time the latter ones will suffer some delay ; in the practice it may be like
    // 4 intents delivered at the same time or so, and the last one will be handled with a 2.5s
    // delay or so. Maybe there is a better way to do this.
    Util.sleep(600);
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    r = null;
  }
}
