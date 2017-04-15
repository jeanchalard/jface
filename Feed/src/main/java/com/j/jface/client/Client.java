package com.j.jface.client;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Future;
import com.j.jface.FutureValue;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.node.GetNodeNameAction;
import com.j.jface.client.action.wear.DeleteAllDataAction;
import com.j.jface.client.action.wear.DeleteDataAction;
import com.j.jface.client.action.wear.GetDataAction;
import com.j.jface.client.action.wear.PutDataAction;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client extends Handler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  public interface GetDataCallback { void run(@NonNull final String path, @NonNull final DataMap data); }

  private static final int MSG_PROCESS_QUEUE = 1;
  private static final int MSG_CONNECT = 2;
  private static final int MSG_RUN_ACTIONS = 3;
  private static final int MSG_DISCONNECT = 4;
  private static final long[] CONNECTION_FAILURES_BACKOFF = { 0, 1000, 10000, 300000 }; // in milliseconds

  @NonNull final private GoogleApiClient mClient;
  @NonNull final private ConcurrentLinkedQueue<Action> mUpdates = new ConcurrentLinkedQueue<>();
  private int mConnectionFailures = 0;

  public Client(@NonNull final Context context)
  {
    super(getInitialLooper());
    mClient = new GoogleApiClient.Builder(context)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API).addApi(LocationServices.API)
     .addApi(Drive.API).addScope(Drive.SCOPE_FILE)
     .build();
  }

  private static Looper getInitialLooper() {
    final HandlerThread handlerThread = new HandlerThread("mClient worker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();
    return handlerThread.getLooper();
  }

  @Override public void handleMessage(final Message msg)
  {
    switch (msg.what)
    {
      case MSG_PROCESS_QUEUE:
        proceed();
        break;
      case MSG_CONNECT:
        mClient.connect();
        break;
      case MSG_RUN_ACTIONS:
        final Action a = mUpdates.poll();
        if (null != a) a.run(mClient);
        proceed();
        break;
      case MSG_DISCONNECT:
        mClient.disconnect();
        break;
    }
  }

  private void proceed()
  {
    removeMessages(MSG_DISCONNECT);
    final int what; final int delay;
    if (!mClient.isConnected()) { what = MSG_CONNECT; delay = 0; }
    else if (mClient.isConnecting()) { what = MSG_PROCESS_QUEUE; delay = 2 * 1000; } // check again in 2 secs
    else if (mUpdates.isEmpty()) { what = MSG_DISCONNECT; delay = 20 * 1000; } // 20 seconds delay to disconnect
    else { what = MSG_RUN_ACTIONS; delay = 0; }
    sendEmptyMessageDelayed(what, delay);
  }

  public void enqueue(@NonNull final Action action)
  {
    mUpdates.add(action);
    proceed();
  }

  @Override public void onConnected(final Bundle bundle) {
    mConnectionFailures = 0;
    proceed();
  }
  @Override public void onConnectionSuspended(final int i) { proceed(); }
  @Override public void onConnectionFailed(@NonNull final ConnectionResult connectionResult)
  {
    if (connectionResult.hasResolution())
    {
      try
      {
        // Requires this mClient has been passed an activity as context.
        connectionResult.startResolutionForResult((Activity)mClient.getContext(), 1);
      }
      catch (final IntentSender.SendIntentException e)
      {
        Log.e("JFACE", "Google API can't resolve its own mess : " + e);
      }
    }
    else if (CONNECTION_FAILURES_BACKOFF.length > ++mConnectionFailures)
      sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, CONNECTION_FAILURES_BACKOFF[mConnectionFailures]);
  }


  /**
   * Data API convenience methods.
   */
  @Nullable public DataMap getData(@NonNull final String path)
  {
    final GetDataAction action = new GetDataAction(this, null, path);
    enqueue(action);
    return action.get();
  }

  public void getData(@NonNull final String path, @NonNull final GetDataCallback callback)
  {
    enqueue(new GetDataAction(this, null, path, callback));
  }

  // Helper methods to put data and forget about it
  public void putData(@NonNull final String path, @NonNull final String key, @NonNull final String value)
  {
    enqueue(new PutDataAction(this, path, key, value));
  }

  public void putData(@NonNull final String path, @NonNull final String key, final boolean value)
  {
    enqueue(new PutDataAction(this, path, key, value));
  }

  public void putData(@NonNull final String path, @NonNull final String key, final long value)
  {
    enqueue(new PutDataAction(this, path, key, value));
  }

  public void putData(@NonNull final String path, @NonNull final String key, final ArrayList<Integer> value)
  {
    enqueue(new PutDataAction(this, path, key, value));
  }

  public void putData(@NonNull final String path, @NonNull final DataMap map)
  {
    enqueue(new PutDataAction(this, path, map));
  }

  public Future<String> getNodeId()
  {
    final GetNodeNameAction action = new GetNodeNameAction(this, null);
    enqueue(action);
    return action;
  }

  public void deleteData(@NonNull final Uri uri)
  {
    enqueue(new DeleteDataAction(this, uri));
  }

  public void clearAllData()
  {
    enqueue(new DeleteAllDataAction(this));
  }
}
