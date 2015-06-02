package com.j.jface.face;

import android.support.annotation.NonNull;
import android.text.format.Time;

import com.j.jface.Const;

public enum Status
{
  COMMUTE_MORNING_平日_J("J : Commute (morning)"),
  COMMUTE_EVENING_平日_J("J : Commute (evening)"),
  HOME_平日_J("J : At home (workday)"),
  HOME_休日_J("J : At home (holiday)"),
  日暮里_平日_J("J : Nippori (workday)"),
  日暮里_休日_J("J : Nippori (holiday)"),
  COMMUTE_MORNING_平日_RIO("Rio : Commute (morning, workday)"),
  COMMUTE_EVENING_平日_RIO("Rio : Commute (evening, workday)"),
  COMMUTE_MORNING_休日_RIO("Rio : Commute (morning, holiday)"),
  COMMUTE_EVENING_休日_RIO("Rio : Commute (evening, workday)"),
  COMMUTE_水曜_RIO("Rio : Work ▶ Juggling"),
  JUGGLING_水曜_RIO("Rio : Juggling ▶ Home"),
  HOME_平日_RIO("Rio : At home (workday)"),
  HOME_休日_RIO("Rio : At home (holiday)"),
  OTHER("Somewhere");

  @NonNull public final String description;

  private static final int DUNNO = 0;
  private static final int 千住大橋 = 1;
  private static final int 六本木 = 2;
  private static final int 日暮里 = 3;
  private static final int 立石 = 4;
  private static final int 三ノ輪 = 5;
  private static final int 東京 = 6;
  private static final int TRAVEL = 7;

  Status(@NonNull final String d) { description = d; }

  private static int getSymbolicLocation(@NonNull final DataStore dataStore)
  {
    if (Const.RIO_MODE)
    {
      final Boolean in立石 = dataStore.isWithinFence(Const.立石_FENCE_NAME);
      if (null == in立石) return DUNNO;
      if (in立石) return 立石;
      final Boolean in三ノ輪 = dataStore.isWithinFence(Const.三ノ輪_FENCE_NAME);
      if (null == in三ノ輪) return DUNNO;
      if (in三ノ輪) return 三ノ輪;
      final Boolean in六本木 = dataStore.isWithinFence(Const.六本木_FENCE_NAME);
      if (null == in六本木) return DUNNO;
      if (in六本木) return 六本木;
    }
    else
    {
      final Boolean in日暮里 = dataStore.isWithinFence(Const.日暮里_FENCE_NAME);
      if (null == in日暮里) return DUNNO;
      if (in日暮里) return 日暮里;
      final Boolean in千住大橋 = dataStore.isWithinFence(Const.千住大橋_FENCE_NAME);
      if (null == in千住大橋) return DUNNO;
      if (in千住大橋) return 千住大橋;
      final Boolean in六本木 = dataStore.isWithinFence(Const.六本木_FENCE_NAME);
      if (null == in六本木) return DUNNO;
      if (in六本木) return 六本木;
    }
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
      case 千住大橋 :
      case 三ノ輪 : return "Home";
      case 六本木 : return "六本木";
      case 立石   : return "立石";
      case 東京   : return "東京";
      default    : return "Somewhere";
    }
  }

  public static Status getStatus_J(@NonNull final Time time, final int symbolicLocation)
  {
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);

    if (日暮里 == symbolicLocation) return workDay ? 日暮里_平日_J : 日暮里_休日_J;

    if (workDay
     && (time.hour >= 8 && time.hour <= 11)
     && (DUNNO == symbolicLocation || 千住大橋 == symbolicLocation)) return COMMUTE_MORNING_平日_J;

    if (workDay
     && ((time.hour >= 18 && time.hour <= 23) || time.hour <= 0)
     && (DUNNO == symbolicLocation || 六本木 == symbolicLocation)) return COMMUTE_EVENING_平日_J;

    if (DUNNO == symbolicLocation || 千住大橋 == symbolicLocation)
      return workDay ? HOME_平日_J : HOME_休日_J;

    return OTHER;
  }

  public static Status getStatus_Rio(@NonNull final Time time, final int symbolicLocation)
  {
    if (TRAVEL == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.SATURDAY);

    if ((time.hour >= 5 && time.hour <= 16)
     && (DUNNO == symbolicLocation || 三ノ輪 == symbolicLocation))
      return workDay ? COMMUTE_MORNING_平日_RIO : COMMUTE_MORNING_休日_RIO;

    if (((time.weekDay == Time.WEDNESDAY && time.hour >= 22) || (time.weekDay == Time.THURSDAY && time.hour < 1))
     && (DUNNO == symbolicLocation || 六本木 == symbolicLocation))
      return JUGGLING_水曜_RIO;

    if ((time.hour >= 17 || time.hour <= 0)
     && (DUNNO == symbolicLocation || 立石 == symbolicLocation))
    {
      if (time.weekDay == Time.WEDNESDAY) return COMMUTE_水曜_RIO;
      return workDay ? COMMUTE_EVENING_平日_RIO : COMMUTE_EVENING_休日_RIO;
    }

    if (DUNNO == symbolicLocation || 三ノ輪 == symbolicLocation)
      return workDay ? HOME_平日_RIO : HOME_休日_RIO;

    return OTHER;
  }

  public static Status getStatus(@NonNull final Time time, @NonNull final DataStore dataStore)
  {
    if (Const.RIO_MODE)
      return getStatus_Rio(time, getSymbolicLocation(dataStore));
    else
      return getStatus_J(time, getSymbolicLocation(dataStore));
  }
}
