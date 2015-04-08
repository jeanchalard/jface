package com.j.jface.face;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.util.Log;

import com.j.jface.Const;

public enum Status
{
  COMMUTE_MORNING_平日("Home, workday, morning", "北千住 ▶ 六本木", null),
  COMMUTE_EVENING_休日("Work, workday, evening", "六本木 ▶ 北千住", null),
  HOME_平日("At home (workday)", "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  HOME_休日("At home (holiday)", "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  日暮里_平日("Nippori (workday)", "日暮里 ▶ 千住大橋", null),
  日暮里_休日("Nippori (holiday)", "日暮里 ▶ 千住大橋", null),
  OTHER("Freestyle", null, null);

  @NonNull public final String description;
  @NonNull public final String header1;
  @Nullable public final String header2;

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

  private static int getSymbolicLocation(@NonNull final DataStore dataStore)
  {
    final Boolean in日暮里 = dataStore.isWithinFence(Const.NIPPORI_FENCE_NAME);
    final Boolean atHome = dataStore.isWithinFence(Const.HOME_FENCE_NAME);
    final Boolean atWork = dataStore.isWithinFence(Const.WORK_FENCE_NAME);
    Log.e("\033[34mSTATUS\033[0m", "Nippori = " + in日暮里 + " ; atHome = " + atHome + " ; atWork = " + atWork);
    if (null == in日暮里) return DUNNO;
    if (in日暮里) return 日暮里;
    if (null == atHome) return DUNNO;
    if (atHome) return HOME;
    if (null == atWork) return DUNNO;
    if (atWork) return WORK;
    // TODO : implement TŌKYŌ
    return TRAVEL;
  }

  public static Status getStatus(@NonNull final Time time, @NonNull final DataStore dataStore) {
    final int symbolicLocation = getSymbolicLocation(dataStore);
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);

    if (日暮里 == symbolicLocation) return workDay ? 日暮里_平日 : 日暮里_休日;

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
