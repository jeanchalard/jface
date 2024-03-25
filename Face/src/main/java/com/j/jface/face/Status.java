package com.j.jface.face;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.format.Time;

import com.j.jface.Const;

public enum Status
{
  COMMUTE_MORNING_平日_J("J : Commute (morning)"),
  COMMUTE_EVENING_平日_J("J : Commute (evening)"),
  HOME_平日_J("J : At home (workday)"),
  HOME_土曜_J("J : At home (saturday)"),
  HOME_日曜_J("J : At home (sunday)"),
  日暮里_平日_J("J : Nippori (workday)"),
  日暮里_休日_J("J : Nippori (holiday)"),
  SHIBUYA_休日_J("J : Shibuya (holiday)"),
  ROPPONGI_土曜_J("J : Roppongi (saturday)"),
  ROPPONGI_日曜_J("J : Roppongi (sunday)"),
  HOME_平日_RIO("Rio : At home (workday)"),
  HOME_休日_RIO("Rio : At home (holiday)"),
  WORK_平日_RIO("Rio : At work (workday)"),
  WORK_休日_RIO("Rio : Commute (holiday)"),
  JUGGLING_木曜_RIO("Rio : Juggling ▶ Home"),
  ROPPONGI_休日_RIO("Rio : Roppongi (holiday)"),
  OTHER("Somewhere");

  @NonNull public final String description;

  // Negative values are used by overrides.
  private static final int DUNNO = 0;
  private static final int 千住大橋 = 1;
  private static final int 六本木 = 2;
  private static final int 日暮里 = 3;
  private static final int 稲城 = 4;
  private static final int 本蓮沼 = 5;
  private static final int 東京 = 6;
  private static final int 渋谷 = 7;
  private static final int SOMEWHERE = 8;

  Status(@NonNull final String d) { description = d; }

  @NonNull public static Status getNextStatusOverride(@NonNull final Time time, @NonNull final DataStore dataStore, @NonNull final TapControl control)
  {
    final Status override = control.getStatusOverride();
    final Status status = override != null ? override : getStatus(time, dataStore);
    switch (status)
    {
      case COMMUTE_MORNING_平日_J : return COMMUTE_EVENING_平日_J;
      case COMMUTE_EVENING_平日_J : return HOME_平日_J;
      case HOME_平日_J : return 日暮里_平日_J;
      case 日暮里_平日_J : return OTHER;

      case HOME_土曜_J : return HOME_日曜_J;
      case HOME_日曜_J : return 日暮里_休日_J;
      case 日暮里_休日_J : return SHIBUYA_休日_J;
      case SHIBUYA_休日_J : return ROPPONGI_土曜_J;
      case ROPPONGI_土曜_J : return ROPPONGI_日曜_J;
      case ROPPONGI_日曜_J : return OTHER;

      case HOME_平日_RIO : return WORK_平日_RIO;
      case WORK_平日_RIO : return JUGGLING_木曜_RIO;
      case JUGGLING_木曜_RIO: return OTHER;

      case HOME_休日_RIO : return WORK_休日_RIO;
      case WORK_休日_RIO : return ROPPONGI_休日_RIO;
      case ROPPONGI_休日_RIO : return OTHER;

      default:
      case OTHER :
        final boolean workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);
        if (Const.RIO_MODE) return workDay ? HOME_平日_RIO : HOME_休日_RIO;
        else return workDay ? COMMUTE_MORNING_平日_J : HOME_土曜_J;
    }
  }

  private static int getSymbolicLocation(@NonNull final DataStore dataStore)
  {
    if (Const.RIO_MODE)
    {
      final Boolean in稲城 = dataStore.isWithinFence(Const.稲城_FENCE_NAME);
      if (null == in稲城) return DUNNO;
      if (in稲城) return 稲城;
      final Boolean in本蓮沼 = dataStore.isWithinFence(Const.本蓮沼_FENCE_NAME);
      if (null == in本蓮沼) return DUNNO;
      if (in本蓮沼) return 本蓮沼;
      final Boolean in渋谷 = dataStore.isWithinFence(Const.渋谷_FENCE_NAME);
      if (null == in渋谷) return DUNNO;
      if (in渋谷) return 渋谷;
    }
    else
    {
      final Boolean in日暮里 = dataStore.isWithinFence(Const.日暮里_FENCE_NAME);
      if (null == in日暮里) return DUNNO;
      if (in日暮里) return 日暮里;
      final Boolean in千住大橋 = dataStore.isWithinFence(Const.千住大橋_FENCE_NAME);
      if (null == in千住大橋) return DUNNO;
      if (in千住大橋) return 千住大橋;
      final Boolean in渋谷 = dataStore.isWithinFence(Const.渋谷_FENCE_NAME);
      if (null == in渋谷) return DUNNO;
      if (in渋谷) return 渋谷;
      final Boolean in六本木 = dataStore.isWithinFence(Const.六本木_FENCE_NAME);
      if (null == in六本木) return DUNNO;
      if (in六本木) return 六本木;
    }
    final Boolean in東京 = dataStore.isWithinFence(Const.東京_FENCE_NAME);
    if (null == in東京) return DUNNO;
    if (in東京) return 東京;
    return SOMEWHERE;
  }

  public static String getSymbolicLocationName(@Nullable final Status statusOverride, @NonNull final DataStore dataStore)
  {
    if (null == statusOverride)
      switch (getSymbolicLocation(dataStore))
      {
        case 稲城 :
        case 千住大橋 :
          return "家";
        case 渋谷:
          return "渋谷";
        case 六本木 :
          return "六本木";
        case 日暮里 :
          return "日暮里";
        case 本蓮沼 :
          return "本蓮沼";
        case 東京 :
          return "東京";
        case SOMEWHERE :
        case DUNNO :
        default:
          return "--";
      }
    switch (statusOverride)
    {
      case COMMUTE_MORNING_平日_J :
      case HOME_平日_J :
      case HOME_土曜_J :
      case HOME_日曜_J :
      case HOME_平日_RIO :
      case HOME_休日_RIO :
        return "家";

      case 日暮里_平日_J :
      case 日暮里_休日_J :
        return "日暮里";

      case COMMUTE_EVENING_平日_J :
      case JUGGLING_木曜_RIO:
        return "渋谷";

      case ROPPONGI_土曜_J:
      case ROPPONGI_日曜_J:
      case ROPPONGI_休日_RIO:
        return "六本木";

      case WORK_平日_RIO :
      case WORK_休日_RIO :
        return "本蓮沼";

      default:
      case OTHER :
        return "--";
    }
  }

  public static Status getStatus_J(@NonNull final Time time, final int symbolicLocation)
  {
    if (SOMEWHERE == symbolicLocation) return OTHER;

    // TODO : figure out national holidays
    final boolean workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.FRIDAY);

    if (日暮里 == symbolicLocation) return workDay ? 日暮里_平日_J : 日暮里_休日_J;

    if (workDay
     && (time.hour >= 7 && time.hour <= 12)
     && (DUNNO == symbolicLocation || 千住大橋 == symbolicLocation)) return COMMUTE_MORNING_平日_J;

    if (workDay
     && ((time.hour >= 18 && time.hour <= 23) || time.hour <= 0)
     && (DUNNO == symbolicLocation || 渋谷 == symbolicLocation)) return COMMUTE_EVENING_平日_J;

    if (!workDay && 渋谷 == symbolicLocation) return SHIBUYA_休日_J;

    if (time.weekDay == Time.SATURDAY && 六本木 == symbolicLocation) return ROPPONGI_土曜_J;
    if (time.weekDay == Time.SUNDAY && 六本木 == symbolicLocation) return ROPPONGI_日曜_J;

    if (DUNNO == symbolicLocation || 千住大橋 == symbolicLocation)
      switch (time.weekDay)
      {
        case Time.SATURDAY : return HOME_土曜_J;
        case Time.SUNDAY : return HOME_日曜_J;
        default : return HOME_平日_J;
      }
    return OTHER;
  }

  public static Status getStatus_Rio(@NonNull final Time time, final int symbolicLocation)
  {
    if (SOMEWHERE == symbolicLocation) return OTHER;

    final boolean workDay;
    // TODO : figure out national holidays
    workDay = (time.weekDay >= Time.MONDAY && time.weekDay <= Time.SATURDAY);

    if (稲城 == symbolicLocation)
      return workDay ? HOME_平日_RIO : HOME_休日_RIO;

    if (本蓮沼 == symbolicLocation)
      return workDay ? WORK_平日_RIO : WORK_休日_RIO;

    if (((time.weekDay == Time.THURSDAY && time.hour >= 19) || (time.weekDay == Time.FRIDAY && time.hour < 1))
     && (DUNNO == symbolicLocation || 渋谷 == symbolicLocation))
      return JUGGLING_木曜_RIO;

    if (!workDay && 六本木 == symbolicLocation) return ROPPONGI_休日_RIO;

    if (DUNNO == symbolicLocation && workDay)
    {
      if (time.hour >= 4 && time.hour <= 12) return HOME_平日_RIO;
      if (time.hour >= 17 && time.hour <= 23) return WORK_平日_RIO;
    }

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
