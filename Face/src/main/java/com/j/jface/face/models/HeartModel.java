package com.j.jface.face.models;

import android.os.SystemClock;

public class HeartModel
{
  public static final int FADE_OUT_MS = 500;
  private long activeSince = Long.MAX_VALUE; // In units of SystemClock.uptimeMillis
  private long activeUntil = Long.MAX_VALUE; // Likewise

  public boolean isActive()
  {
    final long now = SystemClock.uptimeMillis();
    return activeSince < now && now < activeUntil;
  }

  public void start()
  {
    activeSince = SystemClock.uptimeMillis();
    activeUntil = Long.MAX_VALUE;
  }

  public void stop()
  {
    activeUntil = SystemClock.uptimeMillis() + FADE_OUT_MS;
  }

  public long sinceActive()
  {
    return SystemClock.uptimeMillis() - activeSince;
  }

  public long toInactive()
  {
    return activeUntil - SystemClock.uptimeMillis();
  }
}
