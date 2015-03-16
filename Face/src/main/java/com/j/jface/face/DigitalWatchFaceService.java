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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

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
import com.j.jface.R;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DigitalWatchFaceService extends CanvasWatchFaceService
{
  private static final String TAG = "J";
  private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  private static final long NORMAL_UPDATE_RATE_MS = 1000;
  private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

  @NonNull @Override public Engine onCreateEngine()
  {
    return new Engine();
  }

  private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
   GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
  {
    static final int MUTE_ALPHA = 100;
    static final int NORMAL_ALPHA = 255;

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
            if (shouldTimerBeRunning())
            {
              long timeMs = System.currentTimeMillis();
              long delayMs =
               mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
              mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
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
    Drawable mBackground;
    Bitmap mHibiyaIcon;
    Paint mBackgroundPaint;
    Paint mPaint;
    Paint mSecondsPaint;
    Paint mDeparturePaint;
    boolean mMute;
    Time mTime;
    float mYOffset;
    float mDepartureYOffset;
    float mIconToDepartureTextPadding;
    boolean mBackgroundPresent = true;
    private final int mDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_DIGITS;
    private final int mSecondsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECONDS;
    private final int mDepartureColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_DEPARTURE;

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
      Resources resources = DigitalWatchFaceService.this.getResources();
      mYOffset = resources.getDimension(R.dimen.digital_y_offset);
      mDepartureYOffset = resources.getDimension(R.dimen.departure_y_offset);
      mIconToDepartureTextPadding = resources.getDimension(R.dimen.icon_to_departure_text_padding);

      mBackground = resources.getDrawable(R.drawable.bg);
      mHibiyaIcon = ((BitmapDrawable)resources.getDrawable(R.drawable.hibiya)).getBitmap();
      mBackgroundPaint = new Paint();
      mBackgroundPaint.setColor(0xFF000000);
      mPaint = createTextPaint(mDigitsColor, NORMAL_TYPEFACE);
      mSecondsPaint = createTextPaint(mSecondsColor, NORMAL_TYPEFACE);
      mDeparturePaint = createTextPaint(mDepartureColor, NORMAL_TYPEFACE);

      mTime = new Time();
    }

    @Override
    public void onDestroy()
    {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      super.onDestroy();
    }

    @NonNull
    private Paint createTextPaint(final int defaultInteractiveColor, final Typeface typeface)
    {
      Paint paint = new Paint();
      paint.setColor(defaultInteractiveColor);
      paint.setTypeface(typeface);
      paint.setAntiAlias(true);
      return paint;
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
    public void onApplyWindowInsets(final WindowInsets insets)
    {
      super.onApplyWindowInsets(insets);

      // Load resources that have alternate values for round watches.
      Resources resources = DigitalWatchFaceService.this.getResources();
      mPaint.setTextSize(resources.getDimension(R.dimen.time_text_size));
      mSecondsPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.seconds_text_size));
      mDeparturePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.departure_text_size));
    }

    @Override
    public void onPropertiesChanged(@NonNull Bundle properties)
    {
      super.onPropertiesChanged(properties);
      boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
      mPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);
      mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
//      Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
//       + ", low-bit ambient = " + mLowBitAmbient);
    }

    @Override
    public void onTimeTick()
    {
      super.onTimeTick();
//      Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
      invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode)
    {
      super.onAmbientModeChanged(inAmbientMode);
      Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
      adjustPaintColorToCurrentMode(mBackgroundPaint, 0xFF000000, 0xFF000000);
      adjustPaintColorToCurrentMode(mPaint, mDigitsColor,
       DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_DIGITS);
      if (mLowBitAmbient)
      {
        boolean antiAlias = !inAmbientMode;
        mPaint.setAntiAlias(antiAlias);
      }
      invalidate();

      // Whether the timer should be running depends on whether we're in ambient mode (as well
      // as whether we're visible), so we may need to start or stop the timer.
      updateTimer();
    }

    private void adjustPaintColorToCurrentMode(@NonNull Paint paint, int interactiveColor,
                                               int ambientColor)
    {
      paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter)
    {
      Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
      super.onInterruptionFilterChanged(interruptionFilter);

      boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
      // We only need to update once a minute in mute mode.
      setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

      if (mMute != inMuteMode)
      {
        mMute = inMuteMode;
        int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
        mPaint.setAlpha(alpha);
        invalidate();
      }
    }

    public void setInteractiveUpdateRateMs(long updateRateMs)
    {
      if (updateRateMs == mInteractiveUpdateRateMs) return;
      mInteractiveUpdateRateMs = updateRateMs;
      // Stop and restart the timer so the new update rate takes effect immediately.
      if (shouldTimerBeRunning()) updateTimer();
    }

    @Override
    public void onDraw(@NonNull final Canvas canvas, @NonNull final Rect bounds)
    {
      mTime.setToNow();

      // Draw the background.
      // TODO: only update the relevant part of the display.
      if (mBackgroundPresent)
      {
        mBackground.setBounds(0, 0, bounds.width(), bounds.height());
        mBackground.draw(canvas);
      }
      else
        canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

      // Draw the time.
      final float center = bounds.width() / 2;
      final String hours = String.format("%02d", mTime.hour);
      canvas.drawText(hours, center - mPaint.measureText(hours) - 2, mYOffset, mPaint);
      final String minutes = String.format("%02d", mTime.minute);
      canvas.drawText(minutes, center + 2, mYOffset, mPaint);
      if (!isInAmbientMode() && !mMute)
      {
        final float secondsOffset = center + mPaint.measureText(minutes) + 6;
        final String seconds = String.format("%02d", mTime.second);
        canvas.drawText(seconds, secondsOffset, mYOffset, mSecondsPaint);
      }

      // Draw the departures
      final Pair<Departure, Departure> nextDepartures =
       mDataStore.findNextDepartures(Const.日比谷線_北千住_平日, mTime);

      if (null != nextDepartures) // If data is not yet available this returns null
      {
        final String text = String.format("%02d:%02d",
         nextDepartures.first.mTime / 3600,
         (nextDepartures.first.mTime % 3600) / 60)
         + (nextDepartures.first.m始発 ? "始" : "") + " :: "

         + String.format("%02d:%02d",
         nextDepartures.second.mTime / 3600,
         (nextDepartures.second.mTime % 3600) / 60)
         + (nextDepartures.second.m始発 ? "始" : "");

        final float departureOffset = mDepartureYOffset + mDeparturePaint.getTextSize() + 2;
        final float textOffset = center - mDeparturePaint.measureText(text) / 2;
        canvas.drawBitmap(mHibiyaIcon,
         textOffset - mHibiyaIcon.getWidth() - mIconToDepartureTextPadding,
         departureOffset - mHibiyaIcon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
         mBackgroundPaint);
        canvas.drawText("北千住 → 六本木", textOffset, mDepartureYOffset, mDeparturePaint);
        canvas.drawText(text, textOffset, departureOffset, mDeparturePaint);
      }
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer()
    {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      if (shouldTimerBeRunning())
        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    }

    /**
     * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
     * only run when we're visible and in interactive mode.
     */
    private boolean shouldTimerBeRunning()
    {
      return isVisible() && !isInAmbientMode();
    }

    private void setDefaultValuesForMissingConfigKeys(@NonNull final DataMap config)
    {
      if (!config.containsKey(Const.CONFIG_KEY_BACKGROUND))
        config.putBoolean(Const.CONFIG_KEY_BACKGROUND, true);
    }

    private void updateDataItem(@NonNull final String path, @NonNull final DataMap data) {
      if (path.equals(Const.CONFIG_PATH))
        updateUiForConfigDataMap(data);
      else if (path.startsWith(Const.DATA_PATH))
      {
        final String dataName = path.substring(Const.DATA_PATH.length() + 1); // + 1 for the "/"
        for (final String key : data.keySet())
          if (Const.DATA_KEY_DEPLIST.equals(key))
            mDataStore.putDepartureList(dataName, data.getDataMapArrayList(key));
          else
            mDataStore.putGenericData(dataName, data.getString(key));
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
      mBackgroundPresent = config.getBoolean(Const.CONFIG_KEY_BACKGROUND, mBackgroundPresent);
      invalidate();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(final Bundle connectionHint)
    {
      Log.d(TAG, "onConnected: " + connectionHint);
      Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
      updateConfigAndData();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause)
    {
      Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result)
    {
      Log.d(TAG, "onConnectionFailed: " + result);
    }
  }
}
