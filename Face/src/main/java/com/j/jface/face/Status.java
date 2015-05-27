package com.j.jface.face;

import android.support.annotation.NonNull;
import android.text.format.Time;

import com.j.jface.Const;

public enum Status
{
  COMMUTE_MORNING_平日("Home, workday, morning"), // "北千住 ▶ 六本木", "千住大橋 ▶ 町屋"),
  COMMUTE_EVENING_休日("Work, workday, evening"), // "六本木 ▶ 北千住", null),
  HOME_平日("At home (workday)"), // "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  HOME_休日("At home (holiday)"), // "千住大橋 ▶ 日暮里", "千住大橋 ▶ 成田"),
  日暮里_平日("Nippori (workday)"), // "日暮里 ▶ 千住大橋", null),
  日暮里_休日("Nippori (holiday)"), // "日暮里 ▶ 千住大橋", null),
  OTHER("Somewhere"); //, null, null);

  @NonNull public final String description;

  private static final int DUNNO = 0;
  private static final int HOME = 1;
  private static final int WORK = 2;
  private static final int 日暮里 = 3;
  private static final int 東京 = 4;
  private static final int TRAVEL = 5;

  Status(@NonNull final String d) { description = d; }

  private static int getSymbolicLocation(@NonNull final DataStore dataStore)
  {
    final Boolean in日暮里 = dataStore.isWithinFence(Const.日暮里_FENCE_NAME);
    if (null == in日暮里) return DUNNO;
    if (in日暮里) return 日暮里;
    final Boolean atHome = dataStore.isWithinFence(Const.HOME_FENCE_NAME);
    if (null == atHome) return DUNNO;
    if (atHome) return HOME;
    final Boolean atWork = dataStore.isWithinFence(Const.WORK_FENCE_NAME);
    if (null == atWork) return DUNNO;
    if (atWork) return WORK;
    final Boolean in東京 = dataStore.isWithinFence(Const.東京_FENCE_NAME);
    if (null == in東京) return DUNNO;
    if (in東京) return 東京;
    return TRAVEL;
  }

  public static String getSymbolicLocationName(@NonNull final DataStore dataStore)
  {
    switch (getSymbolicLocation(dataStore))
    {
      case 日暮里 : return "日暮里";
      case HOME  : return "Home";
      case WORK  : return "Work";
      case 東京   : return "東京";
      default    : return "Somewhere";
    }
  }

  public static Status getStatus(@NonNull final Time time, @NonNull final DataStore dataStore) {
    final int symbolicLocation = getSymbolicLocation(dataStore);
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);

    if (日暮里 == symbolicLocation) return workDay ? 日暮里_平日 : 日暮里_休日;

    if (workDay
     && (time.hour >= 6 && time.hour <= 11)
     && (DUNNO == symbolicLocation || HOME == symbolicLocation)) return COMMUTE_MORNING_平日;

    if (workDay
     && ((time.hour >= 18 && time.hour <= 23) || time.hour <= 0)
     && (DUNNO == symbolicLocation || WORK == symbolicLocation)) return COMMUTE_EVENING_休日;

    if (DUNNO == symbolicLocation || HOME == symbolicLocation)
      return workDay ? HOME_平日 : HOME_休日;

    return OTHER;
  }
}
