package com.j.jface;

import android.support.annotation.Nullable;

import java.util.Locale;

public class Departure implements Comparable<Departure>
{
  public final int time;
  public final int dTime;
  public final String extra;
  public final String key;
  public final String headSign;
  public final Departure next;

  public Departure(final int time, final String extra, final String key, final Departure next) {
    this.time = time;
    this.dTime = time < 3 * 3600 ? time + 86400 : time;
    this.extra = extra;
    this.key = key;
    this.next = next;
    this.headSign = Const.HEADSIGNS.get(key);
  }

  public String toString() {
    return String.format(Locale.ROOT, "%02d:%02d", time / 3600, (time % 3600) / 60) + extra;
  }

  @Override
  public int compareTo(@Nullable final Departure another)
  {
    if (null == another) return 1;
    final int result = Integer.compare(dTime, another.dTime);
    if (0 != result) return result;
    return extra.compareTo(another.extra);
  }

  public boolean is終了()
  {
    return time < 0;
  }
}
