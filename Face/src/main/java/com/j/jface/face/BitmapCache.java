package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.j.jface.Const;
import com.j.jface.Departure;
import com.j.jface.Util;

public class BitmapCache
{
  private static final int HEIGHT = 30;
  private final Bitmap mCache;
  private final Canvas mCanvas;
  private final Interpolator interpolator = new DecelerateInterpolator(3.0f);
  private final int mSizeNow, mSizeNext, mOffsetNext;
  private final int mVerticalShift;
  public final Departure mNextDeparture;
  private final int mTime;
  private final Paint mPaint;
  private Rect mSrc, mDst;

  public BitmapCache(final float sizeNow, final float sizeNext, final float offsetNext,
                     final Departure nextDeparture, final Paint p)
  {
    mCache = Bitmap.createBitmap(Const.SCREEN_SIZE, HEIGHT, Bitmap.Config.ARGB_8888);
    mCanvas = new Canvas(mCache);
    mSizeNow = (int)Math.ceil(sizeNow);
    mSizeNext = (int)Math.ceil(sizeNext + 1);
    mOffsetNext = (int)offsetNext;
    mSrc = new Rect(); mDst = new Rect();
    mSrc.top = 0; mSrc.bottom = HEIGHT;
    mNextDeparture = nextDeparture;
    mTime = null == nextDeparture ? -1 : nextDeparture.time;
    mPaint = p;
    mVerticalShift = -p.getFontMetricsInt().top;
  }

  public void clear()
  {
    mCache.eraseColor(0);
  }

  public void drawText(final CharSequence text)
  {
    mCanvas.drawText(text, 0, text.length(), 0, mVerticalShift, mPaint);
  }

  public boolean drawOn(final Canvas canvas, final float x, final float y, final Paint p)
  {
    final long timeOfChange = Util.msSinceUTCMidnightForDeparture(mTime + 60);
    final long now = System.currentTimeMillis() % 86400000;
    final long remainingTimeToDeparture = timeOfChange - now; // 10000 - now % 10000;
    final float iVal;
    if (remainingTimeToDeparture < Const.ANIM_DURATION)
      iVal = interpolator.getInterpolation(
       ((float)(Const.ANIM_DURATION - remainingTimeToDeparture)) / Const.ANIM_DURATION);
    else
      iVal = 0;
    final int size = Math.round(mSizeNow + (mSizeNext - mSizeNow) * iVal);
    mSrc.left = Math.round(mOffsetNext * iVal); mSrc.right = mSrc.left + size;
    mDst.left = (int)x; mDst.right = mDst.left + size;
    mDst.top = (int)(y - mVerticalShift); mDst.bottom = mDst.top + HEIGHT;

    canvas.drawBitmap(mCache, mSrc, mDst, p);
    return remainingTimeToDeparture < Const.ANIM_DURATION + 1000;
  }
}
