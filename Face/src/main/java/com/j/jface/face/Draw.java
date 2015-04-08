package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Log;

import com.j.jface.Const;

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
                      @Nullable final Triplet<Departure> departures1,
                      @Nullable final Triplet<Departure> departures2,
                      @NonNull final Status status, @NonNull final Time time, @NonNull final Sensors sensors)
  {
    long start = System.currentTimeMillis();

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

    if (0 == ((AMBIENT_MODE | MUTE_MODE) & modeFlags))
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
    if (null != departures1) // If data is not yet available this is null
    {
      final float lineHeight = drawTools.departurePaint.getTextSize() + 2;
      // Draw header
      canvas.drawText(status.header1, drawTools.departurePosX, drawTools.departurePosY, drawTools.departurePaint);
      // Draw icon
      final float y1 = drawTools.departurePosY + lineHeight;
      drawIcon(drawTools.departurePosX, y1, status, drawTools, canvas);
      // Draw departures
      mustInvalidate = drawDepartureSet(0, departures1, drawTools.departurePosX, y1, drawTools, canvas);

      if (null != departures2)
      {
        // Draw header
        final float y1e = y1 + lineHeight;
        if (null != status.header2)
          canvas.drawText(status.header2, drawTools.departurePosX, y1e, drawTools.departurePaint);
        // Draw icon
        final float y2 = y1e + lineHeight;
        drawIcon(drawTools.departurePosX, y2, status, drawTools, canvas);
        // Draw departures
        mustInvalidate |= drawDepartureSet(1, departures2, drawTools.departurePosX, y2, drawTools, canvas);
      }
    }

    final int borderTextLength = Formatter.formatBorder(mTmpChr, time, sensors.mPressure);
    canvas.drawTextOnPath(mTmpChr, 0, borderTextLength, drawTools.watchContourPath, 0, 0, drawTools.statusPaint);

    long finish = System.currentTimeMillis();
//    Log.e("TIME", "" + (finish - start));

    return mustInvalidate;
  }

  private final static String separator = " ◈ ";
  private boolean drawDepartureSet(final int index, @NonNull final Triplet<Departure> departures,
                                   final float x, final float y,
                                   @NonNull final DrawTools drawTools, @NonNull final Canvas canvas)
  {
    final CharSequence text;
    final float sizeNow, sizeNext, sizeTotal;
    if (null == departures.second)
    {
      text = Formatter.formatDeparture(mTmpSb, departures.first, 0);
      mTmpSb.append(separator);
      mTmpSb.append("終了");
      sizeNow = drawTools.departurePaint.measureText(text, 0, text.length());
      sizeNext = 0;
      sizeTotal = sizeNow;
    }
    else
    {
      // Here starts deep magic manipulating the internal buffer of the formatter
      text = Formatter.formatDeparture(mTmpSb, departures.first, 0);
      mTmpSb.append(separator);
      final int dep2Start = text.length();
      Formatter.formatDeparture(mTmpSb, departures.second, text.length());
      final int dep2Length = text.length();
      mTmpSb.append(separator);
      if (null == departures.third)
        mTmpSb.append("終了");
      else
        Formatter.formatDeparture(mTmpSb, departures.third, text.length());
      sizeNow = drawTools.departurePaint.measureText(text, 0, dep2Length);
      sizeNext = drawTools.departurePaint.measureText(text, dep2Start, text.length());
      sizeTotal = drawTools.departurePaint.measureText(text, 0, text.length());
    }

    BitmapCache cache = mCache[index];
    if (null == cache.mDepartures || departures.first != cache.mDepartures.first)
    {
      cache = new BitmapCache(sizeNow, sizeNext, sizeTotal - sizeNext, departures, drawTools.departurePaint);
      cache.clear();
      cache.drawText(text);
      mCache[index] = cache;
    }
    return cache.drawOn(canvas, x, y, drawTools.imagePaint);
  }

  private static void drawIcon(final float x, final float y, @NonNull final Status status,
                               @NonNull final DrawTools drawTools, @NonNull final Canvas canvas) {
    final Bitmap icon = drawTools.getIconForStatus(status);
    canvas.drawBitmap(icon,
     x - icon.getWidth() - drawTools.iconToDepartureXPadding,
     y - icon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
     drawTools.imagePaint);
  }
}
