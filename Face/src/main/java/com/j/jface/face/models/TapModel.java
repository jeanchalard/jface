package com.j.jface.face.models;

import android.os.SystemClock;

public class TapModel
{
  private static final int TAP_ACTIVE_TIME_MS = 3000;
  private long lastTapTime = Long.MIN_VALUE; // In units of SystemClock.uptimeMillis;
  private long activeSince = Long.MIN_VALUE; // Likewise

  public boolean startTapAndReturnIfActive()
  {
    final long now = SystemClock.uptimeMillis();
    final boolean wasActive = lastTapTime + TAP_ACTIVE_TIME_MS > now;
    lastTapTime = now;
    if (!wasActive) activeSince = now;
    return wasActive;
  }

  public int sinceActive() {
    return (int)(SystemClock.uptimeMillis() - activeSince);
  }

  public int toInactive() {
    return (int)(lastTapTime + TAP_ACTIVE_TIME_MS - SystemClock.uptimeMillis());
  }
}
