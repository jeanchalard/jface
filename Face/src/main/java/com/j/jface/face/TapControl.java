package com.j.jface.face;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.j.jface.Departure;

/**
 * A class to store the status of stuff that is controlled by tapping
 * on the watch face.
 */
public class TapControl
{
  private static final int NO_OVERRIDE = -1;
  private long RESET_TIME_MILLIS = 5 * 60_000;

  private long lastChangeTime = 0;
  private int symbolicLocationOverride = NO_OVERRIDE;
  private long departureTimeOverride = NO_OVERRIDE;
  private boolean showTopic = true;

  public int getSymbolicLocationOverride()
  {
    if (NO_OVERRIDE == symbolicLocationOverride) return symbolicLocationOverride;
    if (SystemClock.elapsedRealtime() > lastChangeTime + RESET_TIME_MILLIS) symbolicLocationOverride = NO_OVERRIDE;
    return symbolicLocationOverride;
  }

  public long getDepartureTimeOverride()
  {
    if (NO_OVERRIDE == departureTimeOverride) return departureTimeOverride;
    if (SystemClock.elapsedRealtime() > lastChangeTime + RESET_TIME_MILLIS) departureTimeOverride = NO_OVERRIDE;
    return departureTimeOverride;
  }

  public boolean showTopic()
  {
    return showTopic;
  }

  public void nextLocation(@NonNull final DataStore dataStore)
  {
    symbolicLocationOverride = Status.getNextLocationOverride(symbolicLocationOverride, dataStore);
    lastChangeTime = SystemClock.elapsedRealtime();
  }

  public void nextDeparture(@Nullable final Departure dep)
  {
    if (null == dep) return;
    Non, là y'a un problème
    departureTimeOverride = dep.time;
  }

  public void toggleTopic()
  {
    showTopic = !showTopic;
  }
}
