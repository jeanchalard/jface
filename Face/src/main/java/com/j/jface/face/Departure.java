package com.j.jface.face;

import android.support.annotation.Nullable;

public class Departure implements Comparable<Departure>
{
  final int mTime;
  final boolean m始発;
  public Departure(final int time, final boolean 始発) {
    mTime = time;
    m始発 = 始発;
  }
  public String toString() {
    return Integer.toString(mTime / 3600) + ":" + Integer.toString((mTime % 3600) / 60) + (m始発 ? "・" : "");
  }

  @Override
  public int compareTo(@Nullable final Departure another)
  {
    if (null == another) return 1;
    final int result = Integer.compare(mTime, another.mTime);
    if (0 != result) return result;
    return Boolean.compare(m始発, another.m始発);
  }
}
