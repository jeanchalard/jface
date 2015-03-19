package com.j.jface.feed;

import android.support.annotation.NonNull;

public class Logger
{
  private static GeofenceTransitionReceiver logger;
  public static void setLogger(final GeofenceTransitionReceiver l) {
    logger = l;
  }
  public static void L(@NonNull final String s) {
    if (null == logger) return;
    logger.Log(s);
  }
}
