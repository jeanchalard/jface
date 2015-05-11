package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

import com.j.jface.Const;
import com.j.jface.Departure;

public class Draw
{
  public static final int BACKGROUND_PRESENT = 1;
  public static final int AMBIENT_MODE = 2;
  public static final int MUTE_MODE = 4;

  private BitmapCache[] mCache;
  public Draw() {
    mCache = new BitmapCache[] { new BitmapCache(0, 0, 0, null, new Paint()),
      new BitmapCache(0, 0, 0, null, new Paint()) };
  }

  final StringBuilder mTmpSb = new StringBuilder(256);
  final char[] mTmpChr = new char[256];
  public boolean draw(@NonNull final DrawTools drawTools, final int modeFlags,
                      @NonNull final Canvas canvas, @NonNull final Rect bounds,
                      @Nullable final Departure departure1, @Nullable final Departure departure2,
                      @NonNull final Status status, @NonNull final Time time, @NonNull final Sensors sensors,
                      @NonNull final String locationDescriptor)
  {
    long start = System.currentTimeMillis();
    boolean drawFull = 0 == ((AMBIENT_MODE | MUTE_MODE) & modeFlags);

    // Draw the background.
    // TODO: only update the relevant part of the display.
    if (0 != (BACKGROUND_PRESENT & modeFlags))
      drawTools.background.draw(canvas);
    else
      canvas.drawRect(0, 0, bounds.width(), bounds.height(), drawTools.imagePaint);

    // Draw the time.
    final float center = bounds.width() / 2;
    Formatter.format2Digits(mTmpSb, time.hour);
    final float hoursSize = drawTools.minutesPaint.measureText(mTmpSb, 0, 2);
    canvas.drawText(mTmpSb, 0, 2, center - hoursSize - 2, drawTools.timePosY, drawTools.minutesPaint);
    Formatter.format2Digits(mTmpSb, time.minute);
    canvas.drawText(mTmpSb, 0, 2, center + 2, drawTools.timePosY, drawTools.minutesPaint);
    final float secondsOffset = drawTools.minutesPaint.measureText(mTmpSb, 0, 2) + 6;

    if (drawFull)
    {
      Formatter.format2Digits(mTmpSb, time.second);
      canvas.drawText(mTmpSb, 0, 2, center + secondsOffset, drawTools.timePosY, drawTools.secondsPaint);
//      final String monthDay = String.format("%02d/%02d", time.month + 1, time.monthDay);
//      final float monthDaySize = drawTools.secondsPaint.measureText(monthDay);
//      canvas.drawText(monthDay, center - hoursSize - monthDaySize, drawTools.timePosY - drawTools.secondsPaint.getTextSize(), drawTools.secondsPaint);
      final String weekDay = Const.WEEKDAYS[time.weekDay];
      final float weekDaySize = drawTools.secondsPaint.measureText(weekDay);
      canvas.drawText(weekDay, center - hoursSize - 6 - weekDaySize, drawTools.timePosY, drawTools.secondsPaint);
    }

    boolean mustInvalidate = false;
    if (null != status.header1)
    {
      final float lineHeight = drawTools.departurePaint.getTextSize() + 2;
      // Draw header
      canvas.drawText(status.header1, drawTools.departurePosX, drawTools.departurePosY, drawTools.departurePaint);
      // Draw icon
      final float y1 = drawTools.departurePosY + lineHeight;
      if (null != departure1)
        drawIcon(drawTools.departurePosX, y1, departure1.key, drawTools, canvas);
      // Draw departures
      mustInvalidate = drawDepartureSet(0, departure1, drawTools.departurePosX, y1, drawTools, canvas);

      // Draw header
      if (null != status.header2)
      {
        final float y1e = y1 + lineHeight;
        canvas.drawText(status.header2, drawTools.departurePosX, y1e, drawTools.departurePaint);
        // Draw icon
        final float y2 = y1e + lineHeight;
        if (null != departure2)
          drawIcon(drawTools.departurePosX, y2, departure2.key, drawTools, canvas);
        // Draw departures
        mustInvalidate |= drawDepartureSet(1, departure2, drawTools.departurePosX, y2, drawTools, canvas);
      }
    }

    final int borderTextLength = Formatter.formatBorder(mTmpChr, time, drawFull ? locationDescriptor : null);
    canvas.drawTextOnPath(mTmpChr, 0, borderTextLength, drawTools.watchContourPath, 0, 0, drawTools.statusPaint);

    long finish = System.currentTimeMillis();
//    Log.e("TIME", "" + (finish - start));

    return mustInvalidate;
  }

  private final static String separator = " ◈ ";
  private boolean drawDepartureSet(final int index, @Nullable final Departure departure,
                                   final float x, final float y,
                                   @NonNull final DrawTools drawTools, @NonNull final Canvas canvas)
  {
    final float sizeNow, sizeNext, sizeTotal;
    final CharSequence text = Formatter.formatFirstDeparture(mTmpSb, departure, 0);
    if (null == departure)
    {
      sizeNow = drawTools.departurePaint.measureText(text, 0, text.length());
      sizeNext = 0;
      sizeTotal = sizeNow;
    }
    else
    {
      Departure nd = departure.next;
      int endOfNextToLast = text.length();
      final int startOfSecond = endOfNextToLast + separator.length();
      for (int d = 1; d < 1 + Const.DISPLAYED_DEPARTURES_PER_LINE; ++d)
      {
        endOfNextToLast = text.length();
        mTmpSb.append(separator);
        Formatter.formatNextDeparture(mTmpSb, nd, text.length());
        if (null == nd) break;
        nd = nd.next;
      }
      sizeNow = drawTools.departurePaint.measureText(text, 0, endOfNextToLast);
      sizeNext = drawTools.departurePaint.measureText(text, startOfSecond, text.length());
      sizeTotal = drawTools.departurePaint.measureText(text, 0, text.length());
    }

    BitmapCache cache = mCache[index];
    if (null == cache.mNextDeparture || departure != cache.mNextDeparture)
    {
      cache = new BitmapCache(sizeNow, sizeNext, sizeTotal - sizeNext, departure, drawTools.departurePaint);
      cache.clear();
      cache.drawText(text);
      mCache[index] = cache;
    }
    return cache.drawOn(canvas, x, y, drawTools.imagePaint);
  }

  private static void drawIcon(final float x, final float y, @NonNull final String key,
                               @NonNull final DrawTools drawTools, @NonNull final Canvas canvas) {
    final Bitmap icon = drawTools.getIconForKey(key);
    canvas.drawBitmap(icon,
     x - icon.getWidth() - drawTools.iconToDepartureXPadding,
     y - icon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
     drawTools.imagePaint);
  }
}
