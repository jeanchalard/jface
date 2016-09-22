package com.j.jface;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Util
{
  public static @NonNull String NonNullString(@Nullable String s)
  {
    return null == s ? "" : s;
  }
}
