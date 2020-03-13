package com.j.jface.face;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WatchFaceService extends CanvasWatchFaceService {
  @NonNull @Override public Engine onCreateEngine()
  {
    return new Engine();
  }

  private class Engine extends CanvasWatchFaceService.Engine implements WatchFace.Invalidator {
    @NonNull private final WatchFace mFace = new WatchFace(WatchFaceService.this, this);

    @Override public void onCreate(@NonNull final SurfaceHolder holder) {
      super.onCreate(holder);
      setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
       .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
       .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
       .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
       .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
       .setShowUnreadCountIndicator(true)
       .setShowSystemUiTime(false)
       .setAcceptsTapEvents(true)
       .build());
    }

    @Override public void onDestroy() { super.onDestroy(); mFace.onDestroy(); }
    @Override public void onVisibilityChanged(final boolean visible) { super.onVisibilityChanged(visible); mFace.onVisibilityChanged(visible); }
    @Override public void onPropertiesChanged(@Nullable final Bundle prop) { super.onPropertiesChanged(prop); mFace.onPropertiesChanged(prop); }
    @Override public void onAmbientModeChanged(final boolean mode) { super.onAmbientModeChanged(mode); mFace.onAmbientModeChanged(mode); }
    @Override public void onInterruptionFilterChanged(final int filt) { super.onInterruptionFilterChanged(filt); mFace.onInterruptionFilterChanged(filt); }
    @Override public void onTimeTick() { super.onTimeTick(); mFace.onTimeTick(); }
    @Override public void onDraw(@NonNull final Canvas c, @NonNull final Rect bounds) { super.onDraw(c, bounds); mFace.onDraw(c, bounds); }
    @Override public void onTapCommand(@TapType final int tapType, final int x, final int y, final long eventTime) {
      if (!mFace.onTapCommand(tapType, x, y, eventTime))
        super.onTapCommand(tapType, x, y, eventTime);
    }
  }
}
