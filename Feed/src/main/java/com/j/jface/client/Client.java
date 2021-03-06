package com.j.jface.client;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.j.jface.client.action.Action;

import java.util.concurrent.ConcurrentLinkedQueue;

import androidx.annotation.NonNull;

public class Client extends Handler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  private static final boolean DEBUG_CLIENT = false;
  private static final int MSG_PROCESS_QUEUE = 1;
  private static final int MSG_CONNECT = 2;
  private static final int MSG_RUN_ACTIONS = 3;
  private static final int MSG_DISCONNECT = 4;
  private static final long CONNECTION_NON_RESPONSIVE_RESET_DELAY = 4000; // ms
  private static final long[] CONNECTION_FAILURES_BACKOFF = { 0, 1000, 10000, 300000 }; // ms

  @NonNull final private String mCreator;
  @NonNull final private GoogleApiClient mClient;
  @NonNull final private ConcurrentLinkedQueue<Action> mUpdates = new ConcurrentLinkedQueue<>();
  private int mConnectionFailures = 0;
  private long mConnectingSince = -1;

  public Client(@NonNull final Context context)
  {
    super(getInitialLooper());
    mClient = new GoogleApiClient.Builder(context)
     .setHandler(this)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Auth.GOOGLE_SIGN_IN_API, new GoogleSignInOptions.Builder().requestScopes(Drive.SCOPE_FILE).build())
     .addApiIfAvailable(Drive.API)
     .build();

    final StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
    mCreator = caller.getClassName().replaceAll(".*\\.", "");
    l("created in " + caller.getMethodName() + ":" + caller.getLineNumber());
  }

  private static Looper getInitialLooper()
  {
    final HandlerThread handlerThread = new HandlerThread("mClient worker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();
    return handlerThread.getLooper();
  }

  final private void l(final String m)
  {
    if (!DEBUG_CLIENT) return;
    Log.e("Client " + mCreator + " {" + Integer.toHexString(System.identityHashCode(this)) + "}", m);
  }
  private String getMsgName(final int msg)
  {
    if (MSG_PROCESS_QUEUE == msg) return "PROCESS_QUEUE";
    else if (MSG_CONNECT == msg) return "CONNECT";
    else if (MSG_RUN_ACTIONS == msg) return "RUN_ACTIONS";
    else if (MSG_DISCONNECT == msg) return "DISCONNECT";
    else return "UNKNOWN MSG !";
  }

  @Override public void handleMessage(final Message msg)
  {
    l("handleMessage " + getMsgName(msg.what));
    switch (msg.what)
    {
      case MSG_PROCESS_QUEUE :
        proceed();
        break;
      case MSG_CONNECT :
        mClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
        mConnectingSince = SystemClock.elapsedRealtime();
        break;
      case MSG_RUN_ACTIONS :
        final Action a = mUpdates.poll();
        if (null != a) a.run(mClient);
        proceed();
        break;
      case MSG_DISCONNECT :
        mClient.disconnect();
        break;
    }
  }

  private void proceed()
  {
    removeMessages(MSG_DISCONNECT);
    final int what; final int delay;
    if (!mClient.isConnected()) { what = MSG_CONNECT; delay = 0; }
    else if (mClient.isConnecting())
    {
      if (mConnectingSince > 0 && mConnectingSince + CONNECTION_NON_RESPONSIVE_RESET_DELAY < SystemClock.elapsedRealtime())
      {
        // The connection is catatonic for some annoying reason. Reset it.
        sendEmptyMessage(MSG_DISCONNECT);
        sendEmptyMessage(MSG_CONNECT);
        delay = 0;
      }
      else
        delay = 2 * 1000; // 2s grace
      what = MSG_PROCESS_QUEUE;
    } // check again in 2 secs
    else if (mUpdates.isEmpty()) { what = MSG_DISCONNECT; delay = 20 * 1000; } // 20 seconds delay to disconnect
    else { what = MSG_RUN_ACTIONS; delay = 0; }
    removeMessages(what);
    l("proceed to " + getMsgName(what) + " in " + delay + "ms");
    sendEmptyMessageDelayed(what, delay);
  }

  public void enqueue(@NonNull final Action action)
  {
    mUpdates.add(action);
    proceed();
  }

  @Override public void onConnected(final Bundle bundle) {
    l("→ connected");
    mConnectionFailures = 0;
    mConnectingSince = -1;
    // Ideally we should call proceed() here but the client reports still #isConnecting() == true at this point,
    // prompting a wait for connection from proceed(). Is this a bug in the GoogleApiClient ? Anyway this is
    // pretty annoying. Wait for the next run loop to continue, at least we'll only wait a frame for it to
    // sort its mess.
    sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, 0);
  }

  @Override public void onConnectionSuspended(final int i) { l("→ suspended"); proceed(); }
  @Override public void onConnectionFailed(@NonNull final ConnectionResult connectionResult)
  {
    l("→ failed");
    if (connectionResult.hasResolution())
    {
      try
      {
        // Requires this mClient has been passed an activity as context.
        final Context context = mClient.getContext();
        if (context instanceof Activity)
          connectionResult.startResolutionForResult((Activity)mClient.getContext(), 1);
        else
          Log.e("JFACE", "Not an activity : can't help Google API resolve its mess");
      }
      catch (final IntentSender.SendIntentException e)
      {
        Log.e("JFACE", "Google API can't resolve its own mess : " + e);
      }
    }
    else if (CONNECTION_FAILURES_BACKOFF.length > ++mConnectionFailures)
      sendEmptyMessageDelayed(MSG_PROCESS_QUEUE, CONNECTION_FAILURES_BACKOFF[mConnectionFailures]);
  }
}
