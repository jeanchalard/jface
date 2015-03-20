package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.j.jface.Const;

public class BitmapCache
{
  private static final int HEIGHT = 25;
  private static final Bitmap sCache = Bitmap.createBitmap(Const.SCREEN_SIZE, HEIGHT, Bitmap.Config.ARGB_8888);
  private static final Canvas sCanvas = new Canvas(sCache);
  private final int mSizeNow, mSizeNext, mOffsetNext;
  private final int mVerticalShift;
  private final int mDate;
  private final Paint mPaint;
  private Rect mSrc, mDst;

  public BitmapCache(final float sizeNow, final float sizeNext, final float offsetNext, final int date, final Paint p)
  {
    mSizeNow = (int)Math.ceil(sizeNow);
    mSizeNext = (int)Math.ceil(sizeNext + 1);
    mOffsetNext = (int)offsetNext;
    mSrc = new Rect(); mDst = new Rect();
    mSrc.top = 0; mSrc.bottom = HEIGHT;
    mDate = -1;
    mPaint = p;
    mVerticalShift = -p.getFontMetricsInt().top;
  }

  public void clear()
  {
    sCache.eraseColor(0);
  }

  public void drawText(final String text, final Paint p)
  {
    sCanvas.drawText(text, 0, mVerticalShift, p);
  }

  public void drawOn(final Canvas canvas, final float x, final float y, final Paint p)
  {
    mSrc.left = 0; mSrc.right = mSizeNow;
    mDst.top = (int)(y - mVerticalShift); mDst.bottom = mDst.top + HEIGHT;
    mDst.left = (int)x; mDst.right = mDst.left + mSizeNow;
    canvas.drawBitmap(sCache, mSrc, mDst, p);
  }
}
