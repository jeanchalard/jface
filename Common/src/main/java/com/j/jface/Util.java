package com.j.jface;

public class Util
{
  public static long msSinceUTCMidnightForDeparture(final int time)
  {
    return ((time + 86400 + Const.SECONDS_TO_UTC) % 86400) * 1000;
  }
}
