package com.j.jface.face;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

public enum Status
{
  COMMUTE_MORNING_平日("Home, workday, morning", "北千住 ▶ 六本木", null),
  COMMUTE_EVENING_休日("Work, workday, evening", "六本木 ▶ 北千住", null),
  HOME_平日("At home (workday)", "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  HOME_休日("At home (holiday)", "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  日暮里_平日("Nippori (workday)", "日暮里 ▶ 千住大橋", null),
  日暮里_休日("Nippori (holiday)", "日暮里 ▶ 千住大橋", null),
  OTHER("Freestyle", null, null);

  public final String description;
  public final String header1;
  public final String header2;

  private static final int DUNNO = 0;
  private static final int HOME = 1;
  private static final int WORK = 2;
  private static final int 日暮里 = 3;
  private static final int TŌKYŌ = 4;
  private static final int TRAVEL = 5;

  private Status(@NonNull final String d, @NonNull final String h1, @NonNull final String h2) {
    description = d;
    header1 = h1;
    header2 = h2;
  }

  private static int getSymbolicLocation(@Nullable final Location location) {
    return DUNNO;
  }

  public static Status getStatus(@NonNull final Time time, @Nullable final Location location) {
    final int symbolicLocation = getSymbolicLocation(location);
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);

    // TODO : take altitude into account to figure out if I'm on 4th floor or have already left
    if (workDay
     && (time.hour >= 8 && time.hour <= 11)
     && (DUNNO == symbolicLocation || HOME == symbolicLocation)) return COMMUTE_MORNING_平日;

    if (workDay
     && ((time.hour >= 18 && time.hour <= 23) || time.hour <= 0)
     && (DUNNO == symbolicLocation || WORK == symbolicLocation)) return COMMUTE_EVENING_休日;

    if (DUNNO == symbolicLocation || HOME == symbolicLocation)
      return workDay ? HOME_平日 : HOME_休日;

    return OTHER;
  }
}
