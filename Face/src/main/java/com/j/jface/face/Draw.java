package com.j.jface.face;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Pair;

public class Draw
{
  public static class Params {
    final boolean isBackgroundPresent;
    final boolean isInAmbientMode;
    final boolean isInMuteMode;
    @Nullable final Pair<Departure, Departure> departures;
    @NonNull final Status status;
    @NonNull final Time time;
    public Params(final boolean backgroundPresent, final boolean inAmbientMode, final boolean inMuteMode,
                  @Nullable final Pair<Departure, Departure> d,  @NonNull final Status s, @NonNull final Time t) {
      isBackgroundPresent = backgroundPresent;
      isInAmbientMode = inAmbientMode;
      isInMuteMode = inMuteMode;
      departures = d;
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
      final float secondsOffset = center + drawTools.minutesPaint.measureText(minutes) + 6;
      final String seconds = String.format("%02d", params.time.second);
      canvas.drawText(seconds, secondsOffset, drawTools.timePosY, drawTools.secondsPaint);
    }

    if (null != params.departures) // If data is not yet available this is null
    {
      final String text = String.format("%02d:%02d",
       params.departures.first.time / 3600,
       (params.departures.first.time % 3600) / 60)
       + (params.departures.first.始発 ? "始" : "") + " :: "

       + String.format("%02d:%02d",
       params.departures.second.time / 3600,
       (params.departures.second.time % 3600) / 60)
       + (params.departures.second.始発 ? "始" : "");

      final float departureOffset = drawTools.departurePosY + drawTools.departurePaint.getTextSize() + 2;
      final float textOffset = center - drawTools.departurePaint.measureText(text) / 2;
      canvas.drawBitmap(drawTools.hibiyaIcon,
       textOffset - drawTools.hibiyaIcon.getWidth() - drawTools.iconToDepartureXPadding,
       departureOffset - drawTools.hibiyaIcon.getHeight() + 5, // + 5 for alignment because I can't be assed to compute it
       drawTools.imagePaint);
      canvas.drawText("北千住 → 六本木", center, drawTools.departurePosY, drawTools.departurePaint);
      canvas.drawText(text, center, departureOffset, drawTools.departurePaint);
    }

    canvas.drawTextOnPath(
     String.format("%04d/%02d/%02d - STATUS STATUS STATUS STATUS STATUS", params.time.year, params.time.month + 1, params.time.monthDay),
     drawTools.watchContourPath, 0, 0, drawTools.statusPaint);
  }
}
