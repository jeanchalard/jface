package com.j.jface;

// import android.support.annotation.Nullable;

public class Departure implements Comparable<Departure>
{
  public final int time;
  public final String extra;

  public Departure(final int time, final String extra) {
    this.time = time;
    this.extra = extra;
  }

  public String toString() {
    return Integer.toString(time / 3600) + ":" + Integer.toString((time % 3600) / 60) + extra;
  }

  @Override
//  public int compareTo(@Nullable final Departure another)
  public int compareTo(final Departure another)
  {
    if (null == another) return 1;
    final int result = Integer.compare(time, another.time);
    if (0 != result) return result;
    return extra.compareTo(another.extra);
  }
}
