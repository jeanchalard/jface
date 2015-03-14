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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sample digital watch face with blinking colons and seconds. In ambient mode, the seconds are
 * replaced with an AM/PM indicator and the colons don't blink. On devices with low-bit ambient
 * mode, the text is drawn without anti-aliasing in ambient mode. On devices which require burn-in
 * protection, the hours are drawn in normal rather than bold. The time is drawn with less contrast
 * and without seconds in mute mode.
 */
public class DigitalWatchFaceService extends CanvasWatchFaceService
{
  private static final String TAG = "J";
  private static final Typeface BOLD_TYPEFACE =
   Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
  private static final Typeface NORMAL_TYPEFACE =
   Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

  /**
   * Update rate in milliseconds for normal (not ambient and not mute) mode. We update every
   * second.
   */
  private static final long NORMAL_UPDATE_RATE_MS = 1000;

  /**
   * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
   */
  private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

  @NonNull
  @Override
  public Engine onCreateEngine()
  {
    return new Engine();
  }

  private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
   GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
  {
    /**
     * Alpha value for drawing time when in mute mode.
     */
    static final int MUTE_ALPHA = 100;

    /**
     * Alpha value for drawing time when not in mute mode.
     */
    static final int NORMAL_ALPHA = 255;

    static final int MSG_UPDATE_TIME = 0;

    /**
     * How often {@link #mUpdateTimeHandler} ticks in milliseconds.
     */
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

    final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver()
    {
      @Override
      public void onReceive(Context context, @NonNull Intent intent)
      {
        mTime.clear(intent.getStringExtra("time-zone"));
        mTime.setToNow();
      }
    };
    boolean mRegisteredTimeZoneReceiver = false;

    Drawable mBackground;
    Paint mBackgroundPaint;
    Paint mPaint;
    Paint mSecondsPaint;
    boolean mMute;
    Time mTime;
    float mYOffset;
    boolean mBackgroundPresent = true;
    private final int mDigitsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_DIGITS;
    private final int mSecondsColor = DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_SECONDS;

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

      mBackground = resources.getDrawable(R.drawable.bg);
      mBackgroundPaint = new Paint();
      mBackgroundPaint.setColor(0xFF000000);
      mPaint = createTextPaint(mDigitsColor, NORMAL_TYPEFACE);
      mSecondsPaint = createTextPaint(mSecondsColor, NORMAL_TYPEFACE);

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

        registerReceiver();

        // Update time zone in case it changed while we weren't visible.
        mTime.clear(TimeZone.getDefault().getID());
        mTime.setToNow();
      }
      else
      {
        unregisterReceiver();

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

    private void registerReceiver()
    {
      if (mRegisteredTimeZoneReceiver)
      {
        return;
      }
      mRegisteredTimeZoneReceiver = true;
      IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
      DigitalWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
    }

    private void unregisterReceiver()
    {
      if (!mRegisteredTimeZoneReceiver)
      {
        return;
      }
      mRegisteredTimeZoneReceiver = false;
      DigitalWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
    }

    @Override
    public void onApplyWindowInsets(final WindowInsets insets)
    {
      super.onApplyWindowInsets(insets);

      // Load resources that have alternate values for round watches.
      Resources resources = DigitalWatchFaceService.this.getResources();
      mPaint.setTextSize(resources.getDimension(R.dimen.time_text_size));
      mSecondsPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.seconds_text_size));
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
      if (updateRateMs == mInteractiveUpdateRateMs)
      {
        return;
      }
      mInteractiveUpdateRateMs = updateRateMs;

      // Stop and restart the timer so the new update rate takes effect immediately.
      if (shouldTimerBeRunning())
      {
        updateTimer();
      }
    }

    private void updatePaintIfInteractive(@Nullable Paint paint, int interactiveColor)
    {
      if (!isInAmbientMode() && paint != null)
      {
        paint.setColor(interactiveColor);
      }
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
      {
        canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
      }

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
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer()
    {
      Log.d(TAG, "updateTimer");
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

    private void updateConfigDataItemAndUiOnStartup()
    {
      DigitalWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
       new DigitalWatchFaceUtil.FetchConfigDataMapCallback()
       {
         @Override
         public void onConfigDataMapFetched(@NonNull DataMap startupConfig)
         {
           // If the DataItem hasn't been created yet or some keys are missing,
           // use the default values.
           setDefaultValuesForMissingConfigKeys(startupConfig);
           DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

           updateUiForConfigDataMap(startupConfig);
         }
       }
      );
    }

    private void setDefaultValuesForMissingConfigKeys(@NonNull final DataMap config)
    {
      if (!config.containsKey(Const.CONFIG_KEY_BACKGROUND))
        config.putBoolean(Const.CONFIG_KEY_BACKGROUND, true);
    }

    @Override // DataApi.DataListener
    public void onDataChanged(@NonNull DataEventBuffer dataEvents)
    {
      try
      {
        for (final DataEvent dataEvent : dataEvents)
        {
          Log.e("\033[31mDATA\033[0m", dataEvent.toString());
          if (dataEvent.getType() != DataEvent.TYPE_CHANGED)
          {
            continue;
          }

          final DataItem dataItem = dataEvent.getDataItem();
          final String path = dataItem.getUri().getPath();
          if (path.equals(Const.CONFIG_PATH))
          {
            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap config = dataMapItem.getDataMap();
            Log.d(TAG, "Config DataItem updated:" + config);
            updateUiForConfigDataMap(config);
          }
          else if (path.equals(Const.DATA_PATH))
          {
            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap config = dataMapItem.getDataMap();
            for (final String key : config.keySet())
              Log.d(TAG, "GOT DATA ! " + key + " = " + config.getString(key));
          }
        }
      } finally
      {
        dataEvents.close();
      }
    }

    private void updateUiForConfigDataMap(@NonNull final DataMap config)
    {
      for (final String key : config.keySet())
      {
        if (!config.containsKey(key)) continue;
        if (Const.CONFIG_KEY_BACKGROUND.equals(key))
        {
          mBackgroundPresent = config.getBoolean(key);
        }
      }
      invalidate();
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnected(final Bundle connectionHint)
    {
      Log.d(TAG, "onConnected: " + connectionHint);
      Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
      updateConfigDataItemAndUiOnStartup();
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
