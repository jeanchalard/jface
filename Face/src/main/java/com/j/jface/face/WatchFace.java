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
import android.support.wearable.watchface.WatchFaceService;
import android.text.TextUtils;
import android.text.format.Time;

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
import com.j.jface.Util;
import com.j.jface.face.models.HeartModel;
import com.j.jface.face.models.TapModel;

import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.support.wearable.watchface.WatchFaceService.PROPERTY_BURN_IN_PROTECTION;
import static android.support.wearable.watchface.WatchFaceService.PROPERTY_LOW_BIT_AMBIENT;

public class WatchFace implements DataClient.OnDataChangedListener {
  public interface Invalidator {
    void invalidate();
  }

  private static final int MSG_UPDATE_TIME = 0;

  // Stuff to speak to
  @NonNull private final Context mContext;
  @NonNull private final Invalidator mInvalidator;
  @NonNull private final Handler mUpdateTimeHandler = new UpdateTimeHandler(this);
  @NonNull private final DataClient mDataClient;
  @NonNull private final DrawTools mDrawTools;
  @NonNull private final DataStore mDataStore = new DataStore();
  @NonNull private final TapControl mTapControl = new TapControl();
  // @NonNull private final Sensors mSensors;

  @NonNull private final TapModel mTapModel = new TapModel();
  @NonNull private final HeartModel mHeartModel = new HeartModel();
  @NonNull private final Draw mDraw;

  // Cache object to save allocations
  @NonNull private final Time mTime = new Time();

  // State for configuration
  private boolean mVisible = true;
  private int mModeFlags = 0; // Default mode mute off, ambient off, burn-in protection off, coarse ambient mode off

  // Business state
  @Nullable private Departure mNextDeparture;

  public WatchFace(@NonNull final Context context, @NonNull final Invalidator invalidator) {
    mContext = context;
    mInvalidator = invalidator;
    mDataClient = Wearable.getDataClient(context, new Wearable.WearableOptions.Builder().setLooper(context.getMainLooper()).build());
    mDrawTools = new DrawTools(context.getResources());
    mDraw = new Draw(context.getResources(), mDrawTools, mDataStore, mTapModel, mHeartModel);
    mDataStore.mBackground = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.bg)).getBitmap();
    mDataClient.addListener(this);
    mTime.setToNow();
//      mSensors = new Sensors(service);
    updateConfigAndData();
  }

  public void onDestroy()
  {
    mDataClient.removeListener(this);
    mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
  }

  public void onPropertiesChanged(@NonNull final Bundle properties)
  {
    if (properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false))
      mModeFlags |= Const.COARSE_AMBIENT_MODE;
    else
      mModeFlags &= ~Const.COARSE_AMBIENT_MODE;
    if (properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false))
      mModeFlags |= Const.BURN_IN_PROTECTION_MODE;
    else
      mModeFlags &= ~Const.BURN_IN_PROTECTION_MODE;
  }

  public void onVisibilityChanged(final boolean visible)
  {
    mVisible = visible;
    if (visible)
    {
      mTimeZoneReceiver.register();
      // Update time zone in case it changed while we weren't visible.
      mTime.clear(TimeZone.getDefault().getID());
      mTime.setToNow();
    }
    else
      mTimeZoneReceiver.unregister();

    // Whether the timer should be running depends on whether the face is visible (as well as
    // whether it's in ambient mode), so the timer may need to be started or stopped.
    updateTimer();
  }

  public void onAmbientModeChanged(final boolean inAmbientMode)
  {
    if (inAmbientMode)
      mModeFlags |= Const.AMBIENT_MODE;
    else
      mModeFlags &= ~Const.AMBIENT_MODE;
    mDrawTools.onAmbientModeChanged(inAmbientMode, 0 != (mModeFlags & Const.COARSE_AMBIENT_MODE));
    /*      if (inAmbientMode)
        mSensors.stop();
      else
        mSensors.start(); */
    invalidate();

    // Whether the timer should be running depends on whether the face is visible (as well as
    // whether it's in ambient mode), so the timer may need to be started or stopped.
    updateTimer();
  }

  public long nextUpdateTime() {
    final long now = mDataStore.currentTimeMillis();
    final boolean animating = mTapModel.toInactive() > 0 || mHeartModel.isActive();
    if (animating) return now;

    final boolean active = mVisible && (0 == (mModeFlags & Const.AMBIENT_MODE));
    mTime.setToNow();
    final int d = (mTime.hour * 3600 + mTime.minute * 60) % 86400;
    final boolean mustWakeForAnimation = (!active && null != mNextDeparture && mNextDeparture.time == d);

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

  public void onTimeTick() {
    invalidate();
  }

  private final Time mDepartureTimeOverrideTmp = new Time();
  public void onDraw(@NonNull final Canvas canvas, @NonNull final Rect bounds)
  {
    final int departureTime;
    mTime.setToNow();
    final long timeOffset = mDataStore.mTimeOffset;
    if (0 != timeOffset)
      mTime.set(mTime.toMillis(true) + timeOffset);
    final long departureTimeOverride = mTapControl.getDepartureTimeOverride();
    if (departureTimeOverride > 0)
    {
      mDepartureTimeOverrideTmp.set(departureTimeOverride);
      departureTime = mDepartureTimeOverrideTmp.hour * 3600 + mDepartureTimeOverrideTmp.minute * 60;
    }
    else departureTime = mTime.hour * 3600 + mTime.minute * 60;
    final Status statusOverride = mTapControl.getStatusOverride();
    final Status status = statusOverride != null ? statusOverride : Status.getStatus(mTime, mDataStore);
    final Departure departure1;
    final Departure departure2;
    switch (status) {
      case COMMUTE_MORNING_平日_J :
        departure1 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_平日, departureTime);
        if (null == departure1)
          departure2 = null;
        else
          departure2 = mDataStore.findClosestDeparture(Const.山手線_日暮里_渋谷方面_平日,
           departure1.time + Const.SECONDS_千住大橋_TO_日暮里);
        break;
      case COMMUTE_EVENING_平日_J :
        departure1 = mDataStore.findClosestDeparture(Const.山手線_渋谷_日暮里方面_平日, departureTime);
        if (null == departure1)
          departure2 = null;
        else
          departure2 = mDataStore.findClosestDeparture(Const.京成線_日暮里_千住大橋方面_平日,
           departure1.time + Const.SECONDS_渋谷_TO_日暮里);
        break;
      case HOME_平日_J :
        departure1 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_平日, departureTime);
        departure2 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_成田方面_平日, departureTime);
        break;
      case HOME_休日_J :
        departure1 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_上野方面_休日, departureTime);
        departure2 = mDataStore.findClosestDeparture(Const.京成線_千住大橋_成田方面_休日, departureTime);
        break;
      case 日暮里_平日_J :
        departure1 = mDataStore.findClosestDeparture(Const.京成線_日暮里_千住大橋方面_平日, departureTime);
        departure2 = mDataStore.findClosestDeparture(Const.山手線_日暮里_渋谷方面_平日, departureTime);
        break;
      case 日暮里_休日_J :
        departure1 = mDataStore.findClosestDeparture(Const.京成線_日暮里_千住大橋方面_休日, departureTime);
        departure2 = mDataStore.findClosestDeparture(Const.山手線_日暮里_渋谷方面_休日, departureTime);
        break;
      case ROPPONGI_休日_J :
        departure1 = mDataStore.findClosestDeparture(Const.日比谷線_六本木_平日, departureTime);
        departure2 = null;
        break;
      case HOME_平日_RIO :
        departure1 = mDataStore.findClosestDeparture(Const.京王線_稲城駅_新宿方面_平日, departureTime);
        departure2 = null;
        break;
      case HOME_休日_RIO :
        departure1 = mDataStore.findClosestDeparture(Const.京王線_稲城駅_新宿方面_休日, departureTime);
        departure2 = null;
        break;
      case WORK_平日_RIO:
        departure1 = mDataStore.findClosestDeparture(Const.都営三田線_本蓮沼_目黒方面_平日, departureTime);
        departure2 = null;
        break;
      case WORK_休日_RIO :
        departure1 = mDataStore.findClosestDeparture(Const.都営三田線_本蓮沼_目黒方面_休日, departureTime);
        departure2 = null;
        break;
      case JUGGLING_木曜_RIO :
      case ROPPONGI_休日_RIO :
        departure1 = mDataStore.findClosestDeparture(Const.大江戸線_六本木_新宿方面_平日, departureTime);
        departure2 = null;
        break;
      default:
        departure1 = null;
        departure2 = null;
    }

    if (mDraw.draw(mDrawTools, mModeFlags, canvas, bounds, mDataStore.mBackground,
     departure1, departure2, status, mTime, /*mSensors,*/ Status.getSymbolicLocationName(statusOverride, mDataStore),
     mTapControl.showUserMessage() ? mDataStore.mUserMessage : "", mDataStore.mUserMessageColors))
      invalidate();

    if (null == departure1)
      mNextDeparture = null;
    else if (null == departure2)
      mNextDeparture = departure1;
    else
      mNextDeparture = departure1.dTime < departure2.dTime ? departure1 : departure2;
  }

  public boolean onTapCommand(@WatchFaceService.TapType final int tapType, final int x, final int y, final long eventTime)
  {
    updateTimer();
    if (WatchFaceService.TAP_TYPE_TAP == tapType)
    {
      final boolean doubleTap = mTapModel.isDoubleTap();
      if (mHeartModel.isActive() && !doubleTap)
      {
        mHeartModel.stop();
        return true;
      }
      if (!mTapModel.startTapAndReturnIfActive()) return true;
      // Determine quadrant. Top quadrant : change location ; bottom quadrant : show/hide message ; left/right : backward/forward departures
      if (x < y) // bottom left triangle
        if (x < Const.SCREEN_SIZE - y) // top left triangle
          mTapControl.prevDeparture(mDataStore, mNextDeparture); // Left
        else
        {
          if (doubleTap)
          {
            mTapControl.addCheckpoint(mDataStore);
            WearData.putDataItem(mDataClient, Const.DATA_KEY_CHECKPOINTS, Util.join(mDataStore.mCheckpoints, "\n"));
          }
          else
            mTapControl.toggleUserMessage(); // Bottom
        }
      else if (x < Const.SCREEN_SIZE - y)
      {
        if (doubleTap)
          mHeartModel.start();
        else
          mTapControl.nextStatus(mDataStore, mTime); // Top
      }
      else
        mTapControl.nextDeparture(mDataStore, mNextDeparture); // Right
      invalidate();
      return true;
    }
    else
    {
      invalidate();
      return false;
    }
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
      mDataStore.mBackground = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.bg)).getBitmap();
  }

  private void updateDataItem(@NonNull final String path, @NonNull final DataMap data)
  {
    if (path.equals(Const.CONFIG_PATH))
      ;
    else if (path.startsWith(Const.DATA_PATH))
    {
      final String dataName = path.substring(Const.DATA_PATH.length() + 1); // + 1 for the "/"
      for (final String key : data.keySet())
      {
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
            mDataStore.mBackground = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.bg)).getBitmap();
            return;
          }
          mDataClient.getFdForAsset(asset).addOnCompleteListener(task ->
           {
             if (!task.isSuccessful()) return;
             mDataStore.mBackground = BitmapFactory.decodeStream(task.getResult().getInputStream());
           }
          );
        }
        else if (Const.DATA_KEY_USER_MESSAGE.equals(key))
        {
          mDataStore.mUserMessage = Util.NonNullString(data.getString(Const.DATA_KEY_USER_MESSAGE));
          mDataStore.mUserMessageColors = Util.intArrayFromNullableArrayList(data.getIntegerArrayList(Const.DATA_KEY_USER_MESSAGE_COLORS));
        }
        else if (Const.DATA_KEY_HEART_MESSAGE.equals(key))
          mDataStore.mHeartMessage = splitArray(data.getString(key));
        else if (Const.DATA_KEY_CHECKPOINTS.equals(key))
          mDataStore.mCheckpoints = splitArray(data.getString(key));
      }
    }
    else if (path.startsWith(Const.LOCATION_PATH))
    {
      final String dataName = path.substring(Const.LOCATION_PATH.length() + 1); // + 1 for the "/"
      mDataStore.putLocationStatus(dataName, data.getBoolean(Const.DATA_KEY_INSIDE));
    }
  }

  private String[] splitArray(@Nullable final String source)
  {
    if (TextUtils.isEmpty(source)) return new String[0];
    return source.split("\n");
  }

  private void updateConfigAndData()
  {
    WearData.fetchData(mDataClient, Const.CONFIG_PATH,
     (path, startupConfig) ->
     {
       setDefaultValuesForMissingConfigKeys(startupConfig);
       WearData.putConfigDataItem(mDataClient, startupConfig);
     }
    );
    final WearData.FetchConfigDataMapCallback dataHandler;
    dataHandler = this::updateDataItem;
    for (final String path : Const.ALL_DEPLIST_DATA_PATHS)
      WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + path, dataHandler);
    for (final String path : Const.ALL_FENCE_NAMES)
      WearData.fetchData(mDataClient, Const.LOCATION_PATH + "/" + path, dataHandler);
    WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_USER_MESSAGE, dataHandler);
    WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_HEART_MESSAGE, dataHandler);
    WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_BACKGROUND, dataHandler);
    WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_DEBUG_TIME_OFFSET, dataHandler);
    WearData.fetchData(mDataClient, Const.DATA_PATH + "/" + Const.DATA_KEY_CHECKPOINTS, dataHandler);
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


  private final TimezoneBroadcastReceiver mTimeZoneReceiver = new TimezoneBroadcastReceiver();
  private final class TimezoneBroadcastReceiver extends BroadcastReceiver
  {
    public void register()
    {
      unregister();
      final IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
      mContext.registerReceiver(mTimeZoneReceiver, filter);
    }
    public void unregister()
    {
      //noinspection EmptyCatchBlock
      try { mContext.unregisterReceiver(mTimeZoneReceiver); }
      catch (IllegalArgumentException e) {} // Non mais
    }
    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent)
    {
      mTime.clear(intent.getStringExtra("time-zone"));
      mTime.setToNow();
    }
  }

  /**
   * Handler to update the time periodically in interactive mode.
   */
  private static class UpdateTimeHandler extends Handler
  {
    @NonNull private final WatchFace mEngine;
    public UpdateTimeHandler(@NonNull final WatchFace engine)
    {
      mEngine = engine;
    }

    @Override public void handleMessage(@NonNull final Message message)
    {
      switch (message.what)
      {
        case MSG_UPDATE_TIME:
          final long nextUpdateTime = mEngine.nextUpdateTime();
          final long now = mEngine.mDataStore.currentTimeMillis();
          this.sendEmptyMessageDelayed(MSG_UPDATE_TIME, nextUpdateTime - now);
          break;
      }
      mEngine.invalidate();
    }
  }

  public void invalidate() { mInvalidator.invalidate(); }
}
