package com.j.jface.face;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Pair;

import com.j.jface.Const;

public class Draw
{
  public static class Params {
    final boolean isBackgroundPresent;
    final boolean isInAmbientMode;
    final boolean isInMuteMode;
    @Nullable final Pair<Departure, Departure> departures1;
    @Nullable final Pair<Departure, Departure> departures2;
    @NonNull final Status status;
    @NonNull final Time time;
    public Params(final boolean backgroundPresent, final boolean inAmbientMode, final boolean inMuteMode,
                  @Nullable final Pair<Departure, Departure> d1, @Nullable final Pair<Departure, Departure> d2,
                  @NonNull final Status s, @NonNull final Time t) {
      isBackgroundPresent = backgroundPresent;
      isInAmbientMode = inAmbientMode;
      isInMuteMode = inMuteMode;
      departures1 = d1;
      departures2 = d2;
      status = s;
      time = t;
    }
  }

  public static void draw(@NonNull final DrawTools drawTools, @NonNull final Params params,
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
      final float departure2y =
       drawDeparturePair(params.departures1, params.status.header1,
        center, drawTools.departurePosY, params, drawTools, canvas);
      if (null != params.departures2)
        drawDeparturePair(params.departures2, params.status.header2,
         center, departure2y, params, drawTools, canvas);
    }

    canvas.drawTextOnPath(
     String.format("%04d/%02d/%02d - " + params.status.description, params.time.year, params.time.month + 1, params.time.monthDay),
     drawTools.watchContourPath, 0, 0, drawTools.statusPaint);
  }

  private static float drawDeparturePair(@NonNull final Pair<Departure, Departure> departures,
                                         @Nullable final String header,
                                         final float center, final float y,
                                         @NonNull final Params params, @NonNull final DrawTools drawTools,
                                         @NonNull Canvas canvas)
  {
    final String text = String.format("%02d:%02d :: %02d:%02d",
     departures.first.time / 3600, (departures.first.time % 3600) / 60,
     departures.second.time / 3600, (departures.second.time % 3600) / 60);

    final Bitmap icon = drawTools.getIconForStatus(params.status);
    final float departureOffset = y + drawTools.departurePaint.getTextSize() + 2;
    final float textOffset = center - drawTools.departurePaint.measureText(text) / 2;
    canvas.drawBitmap(icon,
     textOffset - icon.getWidth() - drawTools.iconToDepartureXPadding,
     departureOffset - icon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
     drawTools.imagePaint);
    if (null != header)
      canvas.drawText(header, center, y, drawTools.departurePaint);
    canvas.drawText(text, center, departureOffset, drawTools.departurePaint);
    return departureOffset + drawTools.departurePaint.getTextSize() + 2;
  }
}
