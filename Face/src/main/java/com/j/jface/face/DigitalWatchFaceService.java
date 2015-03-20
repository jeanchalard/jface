/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.face;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DigitalWatchFaceService extends CanvasWatchFaceService
{
  private static final String TAG = "J";

  private static final long NORMAL_UPDATE_RATE_MS = 1000;
  private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

  @NonNull @Override public Engine onCreateEngine()
  {
    return new Engine();
  }

  private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
   GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
  {
    static final int MSG_UPDATE_TIME = 0;

    long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

    /**
     * Handler to update the time periodically in interactive mode.
     */
    final Handler mUpdateTimeHandler = new Handler()
    {
      @Override
      public void handleMessage(@NonNull Message message)
      {
        switch (message.what)
        {
          case MSG_UPDATE_TIME:
            invalidate();
            final long nextUpdateTime = nextUpdateTime();
            final long now = System.currentTimeMillis();
            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, nextUpdateTime - now);
            break;
        }
      }
    };

    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(DigitalWatchFaceService.this)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API)
     .build();

    final TimezoneBroadcastReceiver mTimeZoneReceiver = new TimezoneBroadcastReceiver();
    private final class TimezoneBroadcastReceiver extends BroadcastReceiver
    {
      public void register()
      {
        unregister();
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        DigitalWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
      }
      public void unregister()
      {
        try { DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver); }
        catch (IllegalArgumentException e) {} // Non mais
      }
      @Override
      public void onReceive(Context context, @NonNull Intent intent)
      {
        mTime.clear(intent.getStringExtra("time-zone"));
        mTime.setToNow();
      }
    }

    final DataStore mDataStore = new DataStore();
    Sensors mSensors;
    Draw mDraw = new Draw();
    DrawTools mDrawTools = new DrawTools(null);
    @NonNull final Time mTime = new Time();
    @Nullable Departure mNextDeparture;
    boolean mIsInMuteMode;
    boolean mIsBackgroundPresent = true;

    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    boolean mLowBitAmbient;

    @Override
    public void onCreate(final SurfaceHolder holder)
    {
      super.onCreate(holder);

      setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
       .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
       .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
       .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_PERSISTENT)
       .setShowUnreadCountIndicator(true)
       .setShowSystemUiTime(false)
       .build());
      mDrawTools = new DrawTools(DigitalWatchFaceService.this.getResources());
      mSensors = new Sensors(DigitalWatchFaceService.this);
      mTime.setToNow();
    }

    @Override
    public void onDestroy()
    {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      super.onDestroy();
    }

    @Override
    public void onVisibilityChanged(boolean visible)
    {
      super.onVisibilityChanged(visible);

      if (visible)
      {
        mGoogleApiClient.connect();
        mTimeZoneReceiver.register();
        // Update time zone in case it changed while we weren't visible.
        mTime.clear(TimeZone.getDefault().getID());
        mTime.setToNow();
      }
      else
      {
        mTimeZoneReceiver.unregister();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
          Wearable.DataApi.removeListener(mGoogleApiClient, this);
          mGoogleApiClient.disconnect();
        }
      }

      // Whether the timer should be running depends on whether we're visible (as well as
      // whether we're in ambient mode), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onPropertiesChanged(@NonNull Bundle properties)
    {
      super.onPropertiesChanged(properties);
      mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
      mDrawTools.onPropertiesChanged(properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false), mLowBitAmbient);
    }

    @Override
    public void onAmbientModeChanged(final boolean inAmbientMode)
    {
      super.onAmbientModeChanged(inAmbientMode);
      mDrawTools.onAmbientModeChanged(inAmbientMode, mLowBitAmbient);
      if (inAmbientMode)
        mSensors.stop();
      else
        mSensors.start();
      invalidate();

      // Whether the timer should be running depends on whether we're in ambient mode (as well
      // as whether we're visible), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter)
    {
      Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
      super.onInterruptionFilterChanged(interruptionFilter);
      boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
      // We only need to update once a minute in mute mode.
      setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);
      mIsInMuteMode = inMuteMode;
    }

    public void setInteractiveUpdateRateMs(long updateRateMs)
    {
      if (updateRateMs == mInteractiveUpdateRateMs) return;
      mInteractiveUpdateRateMs = updateRateMs;
      // Stop and restart the timer so the new update rate takes effect immediately.
      updateTimer();
    }

    public long nextUpdateTime() {
      boolean active = isVisible() && !isInAmbientMode();
      mTime.setToNow();
      final int d = (mTime.hour * 3600 + mTime.minute * 60) % 86400;
      final boolean mustWakeForAnimation = (!active && null != mNextDeparture && mNextDeparture.time == d);

      final long now = System.currentTimeMillis();
      // If active, every second, otherwise every minute
      if (active)
        return now + 1000 - now % 1000;
      else
      {
        long nextFrameTime = now + 60000 - now % 60000 - (mustWakeForAnimation ? Const.ANIM_DURATION + 500 : 0);
        if (nextFrameTime < now) return now + 1000 - now % 1000;
        return nextFrameTime;
      }
    }

    @Override
    public void onDraw(@NonNull final Canvas canvas, @NonNull final Rect bounds)
    {
      mTime.setToNow();
      final Status status = Status.getStatus(mTime, null);
      final Triplet<Departure> departures1;
      final Triplet<Departure> departures2;
      switch (status) {
        case COMMUTE_MORNING_平日:
          departures1 = mDataStore.findNextDepartures(Const.日比谷線_北千住_平日, mTime);
          departures2 = null;
          break;
        case COMMUTE_EVENING_休日:
          departures1 = mDataStore.findNextDepartures(Const.日比谷線_六本木_平日, mTime);
          departures2 = null;
          break;
        case HOME_平日:
          departures1 = mDataStore.findNextDepartures(Const.京成線_上野方面_平日, mTime);
          departures2 = mDataStore.findNextDepartures(Const.京成線_成田方面_平日, mTime);
          break;
        case HOME_休日:
          departures1 = mDataStore.findNextDepartures(Const.京成線_上野方面_休日, mTime);
          departures2 = mDataStore.findNextDepartures(Const.京成線_成田方面_休日, mTime);
          break;
        default:
          departures1 = null;
          departures2 = null;
      }

      final Draw.Params params = new Draw.Params(mIsBackgroundPresent, isInAmbientMode(), mIsInMuteMode,
       mSensors.mPressure, departures1, departures2, status, mTime);
      if (mDraw.draw(mDrawTools, params, canvas, bounds))
        invalidate();

      if (null == departures1)
        mNextDeparture = null;
      else if (null == departures2)
        mNextDeparture = departures1.first;
      else
        mNextDeparture = departures1.first.time < departures2.first.time ? departures1.first : departures2.first;
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer()
    {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    }

    private void setDefaultValuesForMissingConfigKeys(@NonNull final DataMap config)
    {
      if (!config.containsKey(Const.CONFIG_KEY_BACKGROUND))
        config.putBoolean(Const.CONFIG_KEY_BACKGROUND, true);
    }

    private void updateDataItem(@NonNull final String path, @NonNull final DataMap data) {
      Log.e("RECEIVED data", path);
      if (path.equals(Const.CONFIG_PATH))
        updateUiForConfigDataMap(data);
      else if (path.startsWith(Const.DATA_PATH))
      {
        final String dataName = path.substring(Const.DATA_PATH.length() + 1); // + 1 for the "/"
        for (final String key : data.keySet())
          if (Const.DATA_KEY_DEPLIST.equals(key))
            mDataStore.putDepartureList(dataName, data.getDataMapArrayList(key));
          else
          {
            mDataStore.putGenericData(dataName, data.getString(key));
            Log.e("RECEIVED " + key, data.getString(key));
            if (Const.DATA_KEY_ADHOC.equals(key)) {
              mTime.set(Long.parseLong(data.getString(key)));
            }
          }
        updateTimer();
      }
    }

    private void updateConfigAndData()
    {
      DigitalWatchFaceUtil.fetchData(mGoogleApiClient, Const.CONFIG_PATH,
       new DigitalWatchFaceUtil.FetchConfigDataMapCallback()
       {
         @Override
         public void onDataFetched(@NonNull final String path, @NonNull final DataMap startupConfig)
         {
           setDefaultValuesForMissingConfigKeys(startupConfig);
           DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
           updateUiForConfigDataMap(startupConfig);
         }
       }
      );
      final DigitalWatchFaceUtil.FetchConfigDataMapCallback dataHandler =
       new DigitalWatchFaceUtil.FetchConfigDataMapCallback()
       {
         @Override
         public void onDataFetched(@NonNull final String path, @NonNull final DataMap data)
         {
           updateDataItem(path, data);
         }
       };
      for (final String path : Const.ALL_DEPLIST_DATA_PATHS)
        DigitalWatchFaceUtil.fetchData(mGoogleApiClient, Const.DATA_PATH + "/" + path, dataHandler);
    }

    @Override // DataApi.DataListener
    public void onDataChanged(@NonNull DataEventBuffer dataEvents)
    {
      try
      {
        for (final DataEvent dataEvent : dataEvents)
        {
          if (dataEvent.getType() != DataEvent.TYPE_CHANGED) continue;
          final DataItem dataItem = dataEvent.getDataItem();
          final String path = dataItem.getUri().getPath();
          updateDataItem(path, DataMapItem.fromDataItem(dataItem).getDataMap());
        }
      }
      finally
      {
        dataEvents.close();
      }
    }

    private void updateUiForConfigDataMap(@NonNull final DataMap config)
    {
      mIsBackgroundPresent = config.getBoolean(Const.CONFIG_KEY_BACKGROUND, mIsBackgroundPresent);
      invalidate();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(final Bundle connectionHint)
    {
      Log.d(TAG, "onConnected : " + connectionHint);
      Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
      updateConfigAndData();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause)
    {
      Log.d(TAG, "onConnectionSuspended : " + cause);
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result)
    {
      Log.d(TAG, "onConnectionFailed : " + result);
    }
  }
}
