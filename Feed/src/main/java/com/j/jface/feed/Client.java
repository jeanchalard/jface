package com.j.jface.feed;

import android.content.Context;
import android.os.*;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.feed.actions.Action;
import com.j.jface.feed.actions.PutDataAction;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Client extends Handler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  private static final int MSG_PROCESS_QUEUE = 1;
  private static final int MSG_CONNECT = 2;
  private static final int MSG_RUN_ACTIONS = 3;
  private static final int MSG_DISCONNECT = 4;
  private static final long[] CONNECTION_FAILURES_BACKOFF = { 0, 1000, 10000, 300000 }; // in milliseconds

  @NonNull final private GoogleApiClient mClient;
  @NonNull final private ConcurrentLinkedQueue<Action> mUpdates = new ConcurrentLinkedQueue<Action>();
  private int mConnectionFailures = 0;

  public Client(@NonNull final Context context)
  {
    super(getInitialLooper());
    mClient = new GoogleApiClient.Builder(context)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API).addApi(LocationServices.API)
     .build();
  }

  private static Looper getInitialLooper() {
    final HandlerThread handlerThread = new HandlerThread("client worker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();
    return handlerThread.getLooper();
  }
  @Override public void handleMessage(final Message msg)
  {
    switch (msg.what)
    {
      case MSG_PROCESS_QUEUE:
        proceed();
        return;
      case MSG_CONNECT:
        mClient.connect();
        return;
      case MSG_RUN_ACTIONS:
        final Action a = mUpdates.poll();
        if (null != a) a.run(mClient);
        proceed();
        return;
      case MSG_DISCONNECT:
        mClient.disconnect();
        return;
    }
  }

  private void proceed()
  {
    removeMessages(MSG_DISCONNECT);
    final int what; final int delay;
    if (!mClient.isConnected()) { what = MSG_CONNECT; delay = 0; }
    else if (mClient.isConnecting()) { what = MSG_PROCESS_QUEUE; delay = 2 * 1000; } // check again in 2 secs
    else if (mUpdates.isEmpty()) { what = MSG_DISCONNECT; delay = 10 * 1000; } // 10 seconds delay to disconnect
    else { what = MSG_RUN_ACTIONS; delay = 0; }
    sendEmptyMessageDelayed(what, delay);
  }

  public void enqueue(@NonNull final Action action)
  {
    mUpdates.add(action);
    proceed();
  }

  // Helper methods to put data and forget about it
  public void putData(@NonNull final String dataName, @NonNull final String key, @NonNull final String value)
  {
    enqueue(new PutDataAction(dataName, key, value));
  }

  public void putData(@NonNull final String dataName, @NonNull final DataMap map)
  {
    enqueue(new PutDataAction(dataName, map));
  }

  @Override public void onConnected(final Bundle bundle) { mConnectionFailures = 0; proceed(); }
  @Override public void onConnectionSuspended(final int i) { proceed(); }
  @Override public void onConnectionFailed(final ConnectionResult connectionResult)
  {
    if (CONNECTION_FAILURES_BACKOFF.length > ++mConnectionFailures)
      sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, CONNECTION_FAILURES_BACKOFF[mConnectionFailures]);
  }
}