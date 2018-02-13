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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;
import com.j.jface.Departure;
import com.j.jface.R;
import com.j.jface.Util;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DigitalWatchFaceService extends CanvasWatchFaceService
{
  private static final long NORMAL_UPDATE_RATE_MS = 1000;
  private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

  private static final int MSG_UPDATE_TIME = 0;

  @NonNull @Override public Engine onCreateEngine()
  {
    return new Engine();
  }

    /**
     * Handler to update the time periodically in interactive mode.
     */
    private static class UpdateTimeHandler extends Handler
    {
      @NonNull private final Engine mEngine;
      public UpdateTimeHandler(@NonNull final Engine engine)
      {
        mEngine = engine;
      }

      @Override public void handleMessage(@NonNull final Message message)
      {
        switch (message.what)
        {
          case MSG_UPDATE_TIME:
            mEngine.invalidate();
            final long nextUpdateTime = mEngine.nextUpdateTime();
            final long now = mEngine.mDataStore.currentTimeMillis();
            this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, nextUpdateTime - now);
            break;
        }
      }
    }

  private class Engine extends CanvasWatchFaceService.Engine implements DataClient.OnDataChangedListener
  {
    private long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

    private final Handler mUpdateTimeHandler = new UpdateTimeHandler(this);
    private final DataClient mDataClient = Wearable.getDataClient(DigitalWatchFaceService.this,
     new Wearable.WearableOptions.Builder().setLooper(DigitalWatchFaceService.this.getMainLooper()).build()
    );


    private final TimezoneBroadcastReceiver mTimeZoneReceiver = new TimezoneBroadcastReceiver();
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
        //noinspection EmptyCatchBlock
        try { DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver); }
        catch (IllegalArgumentException e) {} // Non mais
      }
      @Override
      public void onReceive(final Context context, @NonNull final Intent intent)
      {
        mTime.clear(intent.getStringExtra("time-zone"));
        mTime.setToNow();
      }
    }

    private final DataStore mDataStore = new DataStore();
//    private Sensors mSensors;
    private Draw mDraw = new Draw();
    private DrawTools mDrawTools = new DrawTools(null);
    @NonNull private final Time mTime = new Time();
    @Nullable private Departure mNextDeparture;
    private int mModeFlags = 0; // Default mode mute off, ambient off

    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    private boolean mLowBitAmbient;

    @Override
    public void onCreate(final SurfaceHolder holder)
    {
      super.onCreate(holder);

      setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
       .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
       .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
       .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
       .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
       .setShowUnreadCountIndicator(true)
       .setShowSystemUiTime(false)
       .setAcceptsTapEvents(true)
       .build());
      mDrawTools = new DrawTools(DigitalWatchFaceService.this.getResources());
      mDataStore.mBackground = ((BitmapDrawable)getResources().getDrawable(R.drawable.bg)).getBitmap();
//      mSensors = new Sensors(DigitalWatchFaceService.this);
      mTime.setToNow();
      mDataClient.addListener(this);
      updateConfigAndData();
    }

    @Override
    public void onDestroy()
    {
      mDataClient.removeListener(this);
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      super.onDestroy();
    }

    @Override
    public void onVisibilityChanged(boolean visible)
    {
      super.onVisibilityChanged(visible);

      if (visible)
      {
        mTimeZoneReceiver.register();
        // Update time zone in case it changed while we weren't visible.
        mTime.clear(TimeZone.getDefault().getID());
        mTime.setToNow();
      }
      else
        mTimeZoneReceiver.unregister();

      // Whether the timer should be running depends on whether we're visible (as well as
      // whether we're in ambient mode), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onPropertiesChanged(@NonNull final Bundle properties)
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
/*      if (inAmbientMode)
        mSensors.stop();
      else
        mSensors.start(); */
      invalidate();

      // Whether the timer should be running depends on whether we're in ambient mode (as well
      // as whether we're visible), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onInterruptionFilterChanged(final int interruptionFilter)
    {
      super.onInterruptionFilterChanged(interruptionFilter);
      boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
      // We only need to update once a minute in mute mode.
      if (inMuteMode)
      {
        setInteractiveUpdateRateMs(MUTE_UPDATE_RATE_MS);
        mModeFlags |= Draw.MUTE_MODE;
      }
      else
      {
        setInteractiveUpdateRateMs(NORMAL_UPDATE_RATE_MS);
        mModeFlags &= ~Draw.MUTE_MODE;
      }
    }

    private void setInteractiveUpdateRateMs(final long updateRateMs)
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

      final long now = mDataStore.currentTimeMillis();
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
    public void onTimeTick() {
      super.onTimeTick();
      invalidate();
    }

    @Override
    public void onDraw(@NonNull final Canvas canvas, @NonNull final Rect bounds)
    {
      mTime.setToNow();
      final long timeOffset = mDataStore.mTimeOffset;
      if (0 != timeOffset)
        mTime.set(mTime.toMillis(true) + timeOffset);
      final Status status = Status.getStatus(mTime, mDataStore);
      final Departure departure1;
      final Departure departure2;
      switch (status) {
        case COMMUTE_MORNING_平日_J :
          departure1 = mDataStore.findClosestDeparture(Const.日比谷線_北千住_平日, mTime);
          departure2 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_平日, mTime);
          break;
        case COMMUTE_EVENING_平日_J :
          departure1 = mDataStore.findClosestDeparture(Const.日比谷線_六本木_平日, mTime);
          departure2 = null;
          break;
        case HOME_平日_J :
          departure1 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_平日, mTime);
          departure2 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_成田方面_平日, mTime);
          break;
        case HOME_休日_J :
          departure1 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_休日, mTime);
          departure2 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_成田方面_休日, mTime);
          break;
        case 日暮里_平日_J :
          departure1 = mDataStore.findClosestDeparture(Const.京成線_日暮里_千住大橋方面_平日, mTime);
          departure2 = null;
          break;
        case 日暮里_休日_J :
          departure1 = mDataStore.findClosestDeparture(Const.京成線_日暮里_千住大橋方面_休日, mTime);
          departure2 = null;
          break;
        case HOME_平日_RIO :
          departure1 = mDataStore.findClosestDeparture(Const.京王線_稲城駅_新宿方面_平日, mTime);
          departure2 = null;
          break;
        case HOME_休日_RIO :
          departure1 = mDataStore.findClosestDeparture(Const.京王線_稲城駅_新宿方面_休日, mTime);
          departure2 = null;
          break;
        case WORK_平日_RIO:
          departure1 = mDataStore.findClosestDeparture(Const.都営三田線_本蓮沼_目黒方面_平日, mTime);
          departure2 = null;
          break;
        case WORK_休日_RIO :
          departure1 = mDataStore.findClosestDeparture(Const.都営三田線_本蓮沼_目黒方面_休日, mTime);
          departure2 = null;
          break;
        case JUGGLING_月曜_RIO :
          departure1 = mDataStore.findClosestDeparture(Const.大江戸線_六本木_新宿方面_平日, mTime);
          departure2 = null;
          break;
        default:
          departure1 = null;
          departure2 = null;
      }

      final int ambientFlag = isInAmbientMode() ? Draw.AMBIENT_MODE : 0;
      if (mDraw.draw(mDrawTools, mModeFlags | ambientFlag, canvas, bounds, mDataStore.mBackground,
       departure1, departure2, status, mTime, /*mSensors,*/ Status.getSymbolicLocationName(mDataStore),
       mDataStore.mTopic, mDataStore.mTopicColors))
        invalidate();

      if (null == departure1)
        mNextDeparture = null;
      else if (null == departure2)
        mNextDeparture = departure1;
      else
        mNextDeparture = departure1.dTime < departure2.dTime ? departure1 : departure2;
    }

    @Override public void onTapCommand(@TapType final int tapType, final int x, final int y, final long eventTime)
    {
      if (WatchFaceService.TAP_TYPE_TAP == tapType)
      {
      }
      else super.onTapCommand(tapType, x, y, eventTime);
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

    private void deleteDataItem(@NonNull final String path)
    {
      if (path.equals(Const.DATA_PATH + "/" + Const.DATA_KEY_BACKGROUND))
        mDataStore.mBackground = ((BitmapDrawable)getResources().getDrawable(R.drawable.bg)).getBitmap();
    }

    private void updateDataItem(@NonNull final String path, @NonNull final DataMap data)
    {
      if (path.equals(Const.CONFIG_PATH))
        ;
      else if (path.startsWith(Const.DATA_PATH))
      {
        final String dataName = path.substring(Const.DATA_PATH.length() + 1); // + 1 for the "/"
        for (final String key : data.keySet())
          if (Const.DATA_KEY_DEPLIST.equals(key))
            mDataStore.putDepartureList(dataName, data.getDataMapArrayList(key));
          else if (Const.DATA_KEY_DEBUG_FENCES.equals(key))
            mDataStore.mDebugFences = data.getLong(key);
          else if (Const.DATA_KEY_DEBUG_TIME_OFFSET.equals(key))
            mDataStore.mTimeOffset = data.getLong(key);
          else if (Const.DATA_KEY_BACKGROUND.equals(key))
          {
            final Asset asset = data.getAsset(key);
            if (null == asset)
            {
              mDataStore.mBackground = ((BitmapDrawable)getResources().getDrawable(R.drawable.bg)).getBitmap();
              return;
            }
            mDataClient.getFdForAsset(asset).addOnCompleteListener(task ->
             {
               if (!task.isSuccessful()) return;
               mDataStore.mBackground = BitmapFactory.decodeStream(task.getResult().getInputStream());
             }
            );
          }
          else if (Const.DATA_KEY_TOPIC.equals(key))
          {
            mDataStore.mTopic = Util.NonNullString(data.getString(Const.DATA_KEY_TOPIC));
            mDataStore.mTopicColors = Util.intArrayFromNullableArrayList(data.getIntegerArrayList(Const.DATA_KEY_TOPIC_COLORS));
          }
      }
      else if (path.startsWith(Const.LOCATION_PATH))
      {
        final String dataName = path.substring(Const.LOCATION_PATH.length() + 1); // + 1 for the "/"
        mDataStore.putLocationStatus(dataName, data.getBoolean(Const.DATA_KEY_INSIDE));
      }
    }

    private void updateConfigAndData()
    {
      DigitalWatchFaceUtil.fetchData(mDataClient, Const.CONFIG_PATH,
       (path, startupConfig) ->
       {
         setDefaultValuesForMissingConfigKeys(startupConfig);
         DigitalWatchFaceUtil.putConfigDataItem(mDataClient, startupConfig);
       }
      );
      final DigitalWatchFaceUtil.FetchConfigDataMapCallback dataHandler;
      dataHandler = this::updateDataItem;
      for (final String path : Const.ALL_DEPLIST_DATA_PATHS)
        DigitalWatchFaceUtil.fetchData(mDataClient, Const.DATA_PATH + "/" + path, dataHandler);
      for (final String path : Const.ALL_FENCE_NAMES)
        DigitalWatchFaceUtil.fetchData(mDataClient, Const.LOCATION_PATH + "/" + path, dataHandler);
      DigitalWatchFaceUtil.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_TOPIC, dataHandler);
      DigitalWatchFaceUtil.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_BACKGROUND, dataHandler);
    }

    @Override // DataClient.OnDataChangedListener
    public void onDataChanged(@NonNull final DataEventBuffer dataEvents)
    {
      try
      {
        for (final DataEvent dataEvent : dataEvents)
        {
          final DataItem dataItem = dataEvent.getDataItem();
          final String path = dataItem.getUri().getPath();
          if (DataEvent.TYPE_DELETED == dataEvent.getType())
            deleteDataItem(path);
          else // TYPE_CHANGED
            updateDataItem(path, DataMapItem.fromDataItem(dataItem).getDataMap());
        }
      }
      finally
      {
        dataEvents.release();
      }
      updateTimer();
    }
  }
}
