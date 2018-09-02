package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.Departure;

public class BitmapCache
{
  private static final int HEIGHT = 200;
  private final Bitmap mCache;
  private final Canvas mCanvas;
//  private final Interpolator interpolator = new DecelerateInterpolator(3.0f);
  private final int mSizeNow, mSizeNext, mOffsetNext;
  private final int mVerticalShift;
  public final Departure mNextDeparture;
//  private final int mTime;
  private final Paint mPaint;
  private Rect mSrc, mDst;

  public BitmapCache(final float sizeNow, final float sizeNext, final float offsetNext, final float sizeTotal,
                     @Nullable final Departure nextDeparture, @NonNull final Paint p)
  {
    mCache = Bitmap.createBitmap((int)Math.ceil(sizeTotal), HEIGHT, Bitmap.Config.ARGB_8888);
    mCanvas = new Canvas(mCache);
    mSizeNow = (int)Math.ceil(sizeNow);
    mSizeNext = (int)Math.ceil(sizeNext + 1);
    mOffsetNext = (int)offsetNext;
    mSrc = new Rect(); mDst = new Rect();
    mSrc.top = 0; mSrc.bottom = HEIGHT;
    mNextDeparture = nextDeparture;
//    mTime = null == nextDeparture ? -1 : nextDeparture.dTime;
    mPaint = p;
    mVerticalShift = -p.getFontMetricsInt().top;
  }

  public int width() { return mSizeNow; }
  public void clear()
  {
    mCache.eraseColor(0);
  }

  public void drawText(@NonNull final CharSequence text, final float x)
  {
    mCanvas.drawText(text, 0, text.length(), x, mVerticalShift, mPaint);
  }

  public boolean drawOn(final Canvas canvas, final float x, final float y, final Paint p)
  {
//    TODO: the following does not work. At all.
//    final long timeOfChange = (mTime + 60) * 1000;
//    final long msSinceLocalMidnight = (System.currentTimeMillis() - Const.MILLISECONDS_TO_UTC) % 86400000;
//    final long remainingTimeToDeparture = timeOfChange - msSinceLocalMidnight; // 10000 - now % 10000;
//    final float iVal;
//    if (remainingTimeToDeparture < Const.ANIM_DURATION && remainingTimeToDeparture > 0)
//      iVal = interpolator.getInterpolation(
//       ((float)(Const.ANIM_DURATION - remainingTimeToDeparture)) / Const.ANIM_DURATION);
//    else
//      iVal = 0;
    final float iVal = 0;
    final int size = Math.round(mSizeNow + (mSizeNext - mSizeNow) * iVal);
    mSrc.left = Math.round(mOffsetNext * iVal); mSrc.right = mSrc.left + size;
    mDst.left = (int)x; mDst.right = mDst.left + size;
    mDst.top = (int)(y - mVerticalShift); mDst.bottom = mDst.top + HEIGHT;

    canvas.drawBitmap(mCache, mSrc, mDst, p);
    return false; // remainingTimeToDeparture < Const.ANIM_DURATION + 1000;
  }
}
