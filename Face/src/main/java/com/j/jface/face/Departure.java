package com.j.jface.face;

import android.support.annotation.Nullable;

public class Departure implements Comparable<Departure>
{
  final int time;
  final boolean 始発;
  public Departure(final int time, final boolean 始発) {
    this.time = time;
    this.始発 = 始発;
  }
  public String toString() {
    return Integer.toString(time / 3600) + ":" + Integer.toString((time % 3600) / 60) + (始発 ? "・" : "");
  }

  @Override
  public int compareTo(@Nullable final Departure another)
  {
    if (null == another) return 1;
    final int result = Integer.compare(time, another.time);
    if (0 != result) return result;
    return Boolean.compare(始発, another.始発);
  }
}
