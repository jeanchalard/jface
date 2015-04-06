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
  private static final int MSG_CONNECT = 2;
  private static final int MSG_RUN_ACTIONS = 3;
  private static final int MSG_DISCONNECT = 4;
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
    final int what;
    if (!mClient.isConnected()) what = MSG_CONNECT;
    else if (mClient.isConnecting()) what = MSG_PROCESS_QUEUE; // check again later
    else if (mUpdates.isEmpty()) what = MSG_DISCONNECT;
    else what = MSG_RUN_ACTIONS;
    sendEmptyMessage(what);
  }

  public void enqueue(@NonNull final Action action)
  {
    mUpdates.add(action);
    proceed();
  }

  @Override public void onConnected(final Bundle bundle) { mConnectionFailures = 0; proceed(); }
  @Override public void onConnectionSuspended(final int i) { proceed(); }
  @Override public void onConnectionFailed(final ConnectionResult connectionResult)
  {
    if (CONNECTION_FAILURES_BACKOFF.length > ++mConnectionFailures)
      sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, CONNECTION_FAILURES_BACKOFF[mConnectionFailures]);
  }
}
