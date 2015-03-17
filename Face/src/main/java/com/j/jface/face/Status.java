package com.j.jface.face;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

public enum Status
{
  MORNING_WORKDAY_AROUND_HOME,
  EVENING_WORKDAY_AROUND_WORK,
  NOWORK_HOME,
  OTHER;

  private static final int DUNNO = 0;
  private static final int HOME = 1;
  private static final int WORK = 2;
  private static final int TŌKYŌ = 3;
  private static final int TRAVEL = 4;

  private static int getSymbolicLocation(@Nullable final Location location) {
    return DUNNO;
  }

  public static Status getStatus(@NonNull final Time time, @Nullable final Location location) {
    final int symbolicLocation = getSymbolicLocation(location);
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);
    // TODO : figure out national holidays
    if (!workDay && (DUNNO == symbolicLocation || HOME == symbolicLocation)) return NOWORK_HOME;

    // TODO : take altitude into account to figure out if I'm on 4th floor or have already left
    if (workDay
     && (time.hour <= 8 && time.hour >= 11)
     && (DUNNO == symbolicLocation || HOME == symbolicLocation)) return MORNING_WORKDAY_AROUND_HOME;

    if (workDay
     && ((time.hour >= 18 && time.hour <= 23) || time.hour <= 0)
     && (DUNNO == symbolicLocation || WORK == symbolicLocation)) return EVENING_WORKDAY_AROUND_WORK;

    return OTHER;
  }
}
