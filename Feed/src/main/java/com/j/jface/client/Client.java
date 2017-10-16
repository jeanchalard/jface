package com.j.jface.client;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInApi;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;
import com.j.jface.Future;
import com.j.jface.Util;
import com.j.jface.client.action.Action;
import com.j.jface.client.action.node.GetNodeNameAction;
import com.j.jface.client.action.wear.DeleteAllDataAction;
import com.j.jface.client.action.wear.DeleteDataAction;
import com.j.jface.client.action.wear.GetBitmapAction;
import com.j.jface.client.action.wear.GetDataAction;
import com.j.jface.client.action.wear.PutDataAction;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client extends Handler implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  public interface GetDataCallback { void run(@NonNull final String path, @NonNull final DataMap data); }
  public interface GetBitmapCallback { void run(@NonNull final String path, @NonNull final String key, @Nullable final Bitmap bitmap); }

  private static final int MSG_PROCESS_QUEUE = 1;
  private static final int MSG_SIGN_IN = 2;
  private static final int MSG_CONNECT = 3;
  private static final int MSG_RUN_ACTIONS = 4;
  private static final int MSG_DISCONNECT = 5;
  private static final long[] CONNECTION_FAILURES_BACKOFF = { 0, 1000, 10000, 300000 }; // in

  private static final int SIGNIN_OFF = 0;
  private static final int SIGNIN_INPROGRESS = 1;
  private static final int SIGNIN_OK = 2;

  @NonNull final private GoogleApiClient mClient;
  @NonNull final private ConcurrentLinkedQueue<Action> mUpdates = new ConcurrentLinkedQueue<>();
  private int mConnectionFailures = 0;
  private int mSignedInState = SIGNIN_OFF;

  public Client(@NonNull final Context context)
  {
    super(getInitialLooper());
    mClient = new GoogleApiClient.Builder(context)
     .setHandler(this)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApiIfAvailable(Wearable.API)
     .addApi(LocationServices.API)
     .addApi(Auth.GOOGLE_SIGN_IN_API, new GoogleSignInOptions.Builder().requestScopes(Drive.SCOPE_FILE).build())
     .addApiIfAvailable(Drive.API)
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
      case MSG_PROCESS_QUEUE :
        proceed();
        break;
      case MSG_CONNECT :
        mClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
        break;
      case MSG_SIGN_IN :
        trySignIn();
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
    else if (mClient.isConnecting() || SIGNIN_INPROGRESS == mSignedInState) { what = MSG_PROCESS_QUEUE; delay = 2 * 1000; } // check again in 2 secs
    else if (SIGNIN_OFF == mSignedInState) { what = MSG_SIGN_IN; delay = 0; }
    else if (mUpdates.isEmpty()) { what = MSG_DISCONNECT; delay = 20 * 1000; } // 20 seconds delay to disconnect
    else { what = MSG_RUN_ACTIONS; delay = 0; }
    sendEmptyMessageDelayed(what, delay);
  }

  private void signInHelper(final GoogleSignInResult res)
  {
    if (res.isSuccess())
    {
      mSignedInState = SIGNIN_OK;
      proceed();
    }
    else
    {
      final Context context = mClient.getContext();
      context.startActivity(Auth.GoogleSignInApi.getSignInIntent(mClient));
    }
  }

  private void trySignIn()
  {
    mSignedInState = SIGNIN_INPROGRESS;
    OptionalPendingResult<GoogleSignInResult> res = Auth.GoogleSignInApi.silentSignIn(mClient);
    if (res.isDone())
      signInHelper(res.get());
    else
      res.setResultCallback((final GoogleSignInResult result) -> signInHelper(result));
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

  public void getBitmap(@NonNull final String path, @NonNull final String key, @Nullable final GetBitmapCallback callback)
  {
    enqueue(new GetBitmapAction(this, path, key, null, callback));
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

  public void putData(@NonNull final String path, @NonNull final String key, final Asset value)
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

  public void deleteData(@NonNull final String path)
  {
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    enqueue(new DeleteDataAction(this, dmRequest.getUri()));
  }

  public void clearAllData()
  {
    enqueue(new DeleteAllDataAction(this));
  }
}
