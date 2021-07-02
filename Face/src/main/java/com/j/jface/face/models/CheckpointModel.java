package com.j.jface.face.models;

import android.os.SystemClock;

public class CheckpointModel
{
  public static final int DISPLAY_TIME_MS = 2500;
  private long lastCheckpoint = Long.MAX_VALUE; // In units of SystemClock.uptimeMillis

  public boolean isActive()
  {
    final long now = SystemClock.uptimeMillis();
    return lastCheckpoint < now && now < lastCheckpoint + DISPLAY_TIME_MS;
  }

  public void checkpoint()
  {
    lastCheckpoint = SystemClock.uptimeMillis();
  }
}
