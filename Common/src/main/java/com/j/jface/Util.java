package com.j.jface;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Arrays;

public class Util
{
  public static @NonNull String NonNullString(@Nullable String s)
  {
    return null == s ? "" : s;
  }

  public static @NonNull int[] concat(int[] a1, int[] a2)
  {
    int[] r = Arrays.copyOf(a1, a1.length + a2.length);
    System.arraycopy(a2, 0, r, a1.length, a2.length);
    return r;
  }

  public static @NonNull <T> T[] concat(T[] a1, T[] a2)
  {
    T[] r = Arrays.copyOf(a1, a1.length + a2.length);
    System.arraycopy(a2, 0, r, a1.length, a2.length);
    return r;
  }
}
