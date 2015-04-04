package com.j.jface.feed;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.feed.actions.Action;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Client extends Handler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  private static final int MSG_PROCESS_QUEUE = 1;
  private static final int MSG_DISCONNECT = 2;
  private static final long[] CONNECTION_FAILURES_BACKOFF = { 0, 1000, 10000, 300000 }; // in milliseconds

  @NonNull final private GoogleApiClient mClient;
  @NonNull final private ConcurrentLinkedQueue<Action> mUpdates = new ConcurrentLinkedQueue<Action>();
  private int mConnectionFailures = 0;

  public Client(@NonNull final Context context)
  {
    super(new HandlerThread("client worker").getLooper());
    mClient = new GoogleApiClient.Builder(context)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API).addApi(LocationServices.API)
     .build();
  }


  @Override public void handleMessage(final Message msg)
  {
    removeMessages(MSG_DISCONNECT);
    switch (msg.what)
    {
      case MSG_PROCESS_QUEUE: processQueue(); return;
      case MSG_DISCONNECT: mClient.disconnect(); return;
    }
  }

  private void processQueue()
  {
    if (mClient.isConnected())
    {
      final Action a = mUpdates.poll();
      if (null != a)
      {
        a.run(mClient);
        runQueue();
      }
      else scheduleDisconnect();
    }
    else if (mClient.isConnecting()) {} // Do nothing, we'll get connected soon
    else mClient.connect();
  }

  public void enqueue(@NonNull final Action action)
  {
    mUpdates.add(action);
    runQueue();
  }

  private void runQueue() { sendEmptyMessage(MSG_PROCESS_QUEUE); }
  private void scheduleDisconnect() { sendEmptyMessageDelayed(MSG_DISCONNECT, 10 * 1000); }
  @Override public void onConnected(final Bundle bundle) { mConnectionFailures = 0; runQueue(); }
  @Override public void onConnectionSuspended(final int i) { runQueue(); }
  @Override public void onConnectionFailed(final ConnectionResult connectionResult)
  {
    if (CONNECTION_FAILURES_BACKOFF.length > ++mConnectionFailures)
      sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, CONNECTION_FAILURES_BACKOFF[mConnectionFailures]);
  }
}
