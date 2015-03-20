package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

import com.j.jface.Const;

public class Draw
{
  BitmapCache mCache;
  public Draw() {
    mCache = new BitmapCache(0, 0, 0, -1, new Paint());
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

  public void draw(@NonNull final DrawTools drawTools, @NonNull final Params params,
                   @NonNull final Canvas canvas, @NonNull final Rect bounds)
  {
    // Draw the background.
    // TODO: only update the relevant part of the display.
    if (params.isBackgroundPresent)
      drawTools.background.draw(canvas);
    else
      canvas.drawRect(0, 0, bounds.width(), bounds.height(), drawTools.imagePaint);

    // Draw the time.
    final float center = bounds.width() / 2;
    final String hours = String.format("%02d", params.time.hour);
    canvas.drawText(hours,
     center - drawTools.minutesPaint.measureText(hours) - 2, drawTools.timePosY,
     drawTools.minutesPaint);
    final String minutes = String.format("%02d", params.time.minute);
    canvas.drawText(minutes, center + 2, drawTools.timePosY, drawTools.minutesPaint);
    if (!params.isInAmbientMode && !params.isInMuteMode)
    {
      final float secondsOffset = drawTools.minutesPaint.measureText(minutes) + 6;
      final String seconds = String.format("%02d", params.time.second);
      canvas.drawText(seconds, center + secondsOffset, drawTools.timePosY, drawTools.secondsPaint);
      final String monthDay = String.format("%02d/%02d", params.time.month + 1, params.time.monthDay);
      final float hoursSize = drawTools.minutesPaint.measureText(hours) + 6;
      final float monthDaySize = drawTools.secondsPaint.measureText(monthDay);
//      canvas.drawText(monthDay, center - hoursSize - monthDaySize, drawTools.timePosY - drawTools.secondsPaint.getTextSize(), drawTools.secondsPaint);
      final String weekDay = Const.WEEKDAYS[params.time.weekDay];
      final float weekDaySize = drawTools.secondsPaint.measureText(weekDay);
      canvas.drawText(weekDay, center - hoursSize - weekDaySize, drawTools.timePosY, drawTools.secondsPaint);
    }

    if (null != params.departures1) // If data is not yet available this is null
    {
      // Draw header
      canvas.drawText(params.status.header1,
       drawTools.departurePosX, drawTools.departurePosY, drawTools.departurePaint);
      // Draw icon
      final float y1 = drawTools.departurePosY + drawTools.departurePaint.getTextSize() + 2;
      drawIcon(drawTools.departurePosX, y1, params, drawTools, canvas);
      // Draw departures
      final float y1e = drawDepartureSet(params.departures1, drawTools.departurePosX, y1, drawTools, canvas);

      if (null != params.departures2)
      {
        // Draw header
        if (null != params.status.header2) canvas.drawText(params.status.header2,
         drawTools.departurePosX, y1e, drawTools.departurePaint);
        // Draw icon
        final float y2 = y1e + drawTools.departurePaint.getTextSize() + 2;
        drawIcon(drawTools.departurePosX, y2, params, drawTools, canvas);
        // Draw departures
        drawDepartureSet(params.departures2, drawTools.departurePosX, y2, drawTools, canvas);
      }
    }

    canvas.drawTextOnPath(
     String.format("%04d/%02d/%02d - %.1fhPa", params.time.year, params.time.month + 1, params.time.monthDay, params.pressure),
     drawTools.watchContourPath, 0, 0, drawTools.statusPaint);
  }

  private final static String separator = " ◈ ";
  private float drawDepartureSet(@NonNull final Triplet<Departure> departures,
                                        final float x, final float y,
                                        @NonNull final DrawTools drawTools, @NonNull final Canvas canvas)
  {
    final String text;
    final float sizeNow, sizeNext, sizeTotal;
    if (null == departures.second)
    {
      text = String.format("%02d:%02d%s ◈ 終了",
       departures.first.time / 3600, (departures.first.time % 3600) / 60, departures.first.extra);
      sizeNow = drawTools.departurePaint.measureText(text);
      sizeNext = 0;
      sizeTotal = sizeNow;
    }
    else
    {
      final String text1 = formatDeparture(departures.first);
      final String text2 = formatDeparture(departures.second);
      final String text3 = null == departures.third ? "終了" : formatDeparture(departures.third);
      text = text1 + separator + text2 + separator + text3;
      sizeNow = drawTools.departurePaint.measureText(text1 + separator + text2);
      sizeNext = drawTools.departurePaint.measureText(text2 + separator + text3);
      sizeTotal = drawTools.departurePaint.measureText(text);
    }

    if (departures.first.time != mCache.mTime)
    {
      mCache = new BitmapCache(sizeNow, sizeNext, sizeTotal - sizeNext, departures.first.time,
       drawTools.departurePaint);
      mCache.clear();
      mCache.drawText(text);
    }
    mCache.drawOn(canvas, x, y, drawTools.imagePaint);
    return y + drawTools.departurePaint.getTextSize() + 2;
  }

  private static String formatDeparture(final Departure dep)
  {
    return String.format("%02d:%02d%s", dep.time / 3600, (dep.time % 3600) / 60, dep.extra);
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
