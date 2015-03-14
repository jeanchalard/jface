package com.j.jface.feed;

import android.support.annotation.NonNull;

public class Logger
{
  private static JFaceDataFeed logger;
  public static void setLogger(final JFaceDataFeed l) {
    logger = l;
  }
  public static void L(@NonNull final String s) {
    if (null == logger) return;
    logger.Log(s);
  }
}
