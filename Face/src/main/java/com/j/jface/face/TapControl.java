package com.j.jface.face;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.format.Time;

import com.j.jface.Const;
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
  private Status statusOverride = null;
  private long departureTimeOverride = NO_OVERRIDE;
  private boolean showUserMessage = true;

  @Nullable public Status getStatusOverride()
  {
    if (null == statusOverride) return null;
    if (SystemClock.elapsedRealtime() > lastChangeTime + RESET_TIME_MILLIS) statusOverride = null;
    return statusOverride;
  }

  public long getDepartureTimeOverride()
  {
    if (NO_OVERRIDE == departureTimeOverride) return departureTimeOverride;
    if (SystemClock.elapsedRealtime() > lastChangeTime + RESET_TIME_MILLIS) departureTimeOverride = NO_OVERRIDE;
    return departureTimeOverride;
  }

  public boolean showUserMessage()
  {
    return showUserMessage;
  }

  public void nextStatus(@NonNull final DataStore dataStore, @NonNull final Time now)
  {
    statusOverride = Status.getNextStatusOverride(now, dataStore, this);
    lastChangeTime = SystemClock.elapsedRealtime();
  }

  private long makeTime(final int time)
  {
    final long now = System.currentTimeMillis() + Const.MILLISECONDS_TO_UTC;
    return 1000 * time + now - now % 86_400_000 - Const.MILLISECONDS_TO_UTC;
  }

  public void prevDeparture(@NonNull final DataStore dataStore, @Nullable final Departure nextDeparture)
  {
    if (null == nextDeparture) return;
    final Departure prevDeparture = dataStore.findPrevDeparture(nextDeparture);
    if (null == prevDeparture) return;
    departureTimeOverride = makeTime(prevDeparture.time);
    lastChangeTime = SystemClock.elapsedRealtime();
  }

  public void nextDeparture(@NonNull final DataStore dataStore, @Nullable final Departure nextDeparture)
  {
    if (null == nextDeparture || null == nextDeparture.next) return;
    departureTimeOverride = makeTime(nextDeparture.time + 60);
    lastChangeTime = SystemClock.elapsedRealtime();
  }

  public void toggleUserMessage()
  {
    lastChangeTime = 1;
    showUserMessage = !showUserMessage;
  }
}
