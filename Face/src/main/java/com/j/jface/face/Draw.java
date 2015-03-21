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
  private BitmapCache[] mCache;
  private final Formatter mFormatter = new Formatter();
  public Draw() {
    mCache = new BitmapCache[] { new BitmapCache(0, 0, 0, null, new Paint()),
      new BitmapCache(0, 0, 0, null, new Paint()) };
  }

  public static class Params {
    public final boolean isBackgroundPresent;
    public final boolean isInAmbientMode;
    public final boolean isInMuteMode;
    public final float pressure;
    @Nullable public final Triplet<Departure> departures1;
    @Nullable public final Triplet<Departure> departures2;
    @NonNull public final Status status;
    @NonNull public final Time time;
    public Params(final boolean backgroundPresent, final boolean inAmbientMode, final boolean inMuteMode,
                  final float p,
                  @Nullable final Triplet<Departure> d1, @Nullable final Triplet<Departure> d2,
                  @NonNull final Status s, @NonNull final Time t) {
      isBackgroundPresent = backgroundPresent;
      isInAmbientMode = inAmbientMode;
      isInMuteMode = inMuteMode;
      pressure = p;
      departures1 = d1;
      departures2 = d2;
      status = s;
      time = t;
    }
  }

  public boolean draw(@NonNull final DrawTools drawTools, @NonNull final Params params,
                      @NonNull final Canvas canvas, @NonNull final Rect bounds)
  {
    long start = System.currentTimeMillis();

    // Draw the background.
    // TODO: only update the relevant part of the display.
    if (params.isBackgroundPresent)
      drawTools.background.draw(canvas);
    else
      canvas.drawRect(0, 0, bounds.width(), bounds.height(), drawTools.imagePaint);

    // Draw the time.
    final float center = bounds.width() / 2;
    final CharSequence hours = mFormatter.format2Digits(params.time.hour);
    canvas.drawText(hours, 0, 2,
     center - drawTools.minutesPaint.measureText(hours, 0, 2) - 2, drawTools.timePosY,
     drawTools.minutesPaint);
    final CharSequence minutes = mFormatter.format2Digits(params.time.minute);
    canvas.drawText(minutes, 0, 2, center + 2, drawTools.timePosY, drawTools.minutesPaint);

    if (!params.isInAmbientMode && !params.isInMuteMode)
    {
      final float secondsOffset = drawTools.minutesPaint.measureText(minutes, 0, 2) + 6;
      final CharSequence seconds = mFormatter.format2Digits(params.time.second);
      canvas.drawText(seconds, 0, 2, center + secondsOffset, drawTools.timePosY, drawTools.secondsPaint);
      final float hoursSize = drawTools.minutesPaint.measureText(hours, 0, 2) + 6;
//      final String monthDay = String.format("%02d/%02d", params.time.month + 1, params.time.monthDay);
//      final float monthDaySize = drawTools.secondsPaint.measureText(monthDay);
//      canvas.drawText(monthDay, center - hoursSize - monthDaySize, drawTools.timePosY - drawTools.secondsPaint.getTextSize(), drawTools.secondsPaint);
      final String weekDay = Const.WEEKDAYS[params.time.weekDay];
      final float weekDaySize = drawTools.secondsPaint.measureText(weekDay);
      canvas.drawText(weekDay, center - hoursSize - weekDaySize, drawTools.timePosY, drawTools.secondsPaint);
    }

    boolean mustInvalidate = false;
    if (null != params.departures1) // If data is not yet available this is null
    {
      final float lineHeight = drawTools.departurePaint.getTextSize() + 2;
      // Draw header
      canvas.drawText(params.status.header1,
       drawTools.departurePosX, drawTools.departurePosY, drawTools.departurePaint);
      // Draw icon
      final float y1 = drawTools.departurePosY + lineHeight;
      drawIcon(drawTools.departurePosX, y1, params, drawTools, canvas);
      // Draw departures
      mustInvalidate |= drawDepartureSet(0, params.departures1, drawTools.departurePosX, y1, drawTools, canvas);

      if (null != params.departures2)
      {
        // Draw header
        final float y1e = y1 + lineHeight;
        if (null != params.status.header2) canvas.drawText(params.status.header2,
         drawTools.departurePosX, y1e, drawTools.departurePaint);
        // Draw icon
        final float y2 = y1e + lineHeight;
        drawIcon(drawTools.departurePosX, y2, params, drawTools, canvas);
        // Draw departures
        mustInvalidate |= drawDepartureSet(1, params.departures2, drawTools.departurePosX, y2, drawTools, canvas);
      }
    }

    final String borderText = mFormatter.formatBorder(params).toString();
    canvas.drawTextOnPath(borderText, drawTools.watchContourPath, 0, 0, drawTools.statusPaint);

    long finish = System.currentTimeMillis();
    Log.e("TIME", "" + (finish - start));

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
      text = mFormatter.formatDeparture(departures.first, 0);
      mFormatter.append(separator);
      mFormatter.append("終了");
      sizeNow = drawTools.departurePaint.measureText(text, 0, text.length());
      sizeNext = 0;
      sizeTotal = sizeNow;
    }
    else
    {
      // Here starts deep magic manipulating the internal buffer of the formatter
      text = mFormatter.formatDeparture(departures.first, 0);
      final int dep1Length = text.length();
      mFormatter.append(separator);
      final int dep2Start = text.length();
      final CharSequence text2 = mFormatter.formatDeparture(departures.second, text.length());
      final int dep2Length = text.length();
      mFormatter.append(separator);
      if (null == departures.third)
        mFormatter.append("終了");
      else
        mFormatter.formatDeparture(departures.third, text.length());
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

  private static void drawIcon(final float x, final float y, @NonNull final Params params,
                               @NonNull final DrawTools drawTools, @NonNull final Canvas canvas) {
    final Bitmap icon = drawTools.getIconForStatus(params.status);
    canvas.drawBitmap(icon,
     x - icon.getWidth() - drawTools.iconToDepartureXPadding,
     y - icon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
     drawTools.imagePaint);
  }
}
