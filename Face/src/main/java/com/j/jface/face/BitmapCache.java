package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.j.jface.Const;

public class BitmapCache
{
  private static final int HEIGHT = 25;
  private static final int ANIM_DURATION = 2000; // milliseconds
   private static final long SECONDS_TO_UTC = -9 * 3600;
  private static final Bitmap sCache = Bitmap.createBitmap(Const.SCREEN_SIZE, HEIGHT, Bitmap.Config.ARGB_8888);
  private static final Canvas sCanvas = new Canvas(sCache);
  private final Interpolator interpolator = new DecelerateInterpolator(1.0f);
  private final int mSizeNow, mSizeNext, mOffsetNext;
  private final int mVerticalShift;
  public final int mTime;
  private final Paint mPaint;
  private Rect mSrc, mDst;

  public BitmapCache(final float sizeNow, final float sizeNext, final float offsetNext, final int time, final Paint p)
  {
    mSizeNow = (int)Math.ceil(sizeNow);
    mSizeNext = (int)Math.ceil(sizeNext + 1);
    mOffsetNext = (int)offsetNext;
    mSrc = new Rect(); mDst = new Rect();
    mSrc.top = 0; mSrc.bottom = HEIGHT;
    mTime = time;
    mPaint = p;
    mVerticalShift = -p.getFontMetricsInt().top;
  }

  public void clear()
  {
    sCache.eraseColor(0);
  }

  public void drawText(final String text)
  {
    sCanvas.drawText(text, 0, mVerticalShift, mPaint);
  }

  public boolean drawOn(final Canvas canvas, final float x, final float y, final Paint p)
  {
    final long timeOfChange = ((mTime + 86400 + 60 + SECONDS_TO_UTC) % 86400) * 1000;
    final long now = System.currentTimeMillis() % 86400000;
    final long remainingTimeToDeparture = timeOfChange - now; // 10000 - now % 10000;
    final float iVal;
    if (remainingTimeToDeparture < ANIM_DURATION)
      iVal = interpolator.getInterpolation(((float)(ANIM_DURATION - remainingTimeToDeparture)) / ANIM_DURATION);
    else
      iVal = 0;
    final int size = Math.round(mSizeNow + (mSizeNext - mSizeNow) * iVal);
    mSrc.left = Math.round(mOffsetNext * iVal); mSrc.right = mSrc.left + size;
    mDst.left = (int)x; mDst.right = mDst.left + size;
    mDst.top = (int)(y - mVerticalShift); mDst.bottom = mDst.top + HEIGHT;

    canvas.drawBitmap(sCache, mSrc, mDst, p);
    return remainingTimeToDeparture < ANIM_DURATION + 1000;
  }
}