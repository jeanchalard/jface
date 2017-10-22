package com.j.jface;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class Util
{
  public static @NonNull String NonNullString(@Nullable String s)
  {
    return null == s ? "" : s;
  }

  public static int[] intArrayFromNullableArrayList(@Nullable final ArrayList<Integer> list)
  {
    if (null == list) return new int[0];
    final int[] array = new int[list.size()];
    for (int i = 0; i < array.length; ++i) array[i] = list.get(i);
    return array;
  }
  public static ArrayList<Integer> arrayListFromIntArray(@NonNull int a[])
  {
    final ArrayList<Integer> al = new ArrayList<>(a.length);
    for (int x : a) al.add(x);
    return al;
  }

  public static void sleep(final long ms)
  {
    try { Thread.sleep(ms); }
    catch (final InterruptedException e) { /* go f yourself */ }
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

  public static void copy(@NonNull final InputStream in, @NonNull final OutputStream out) throws IOException
  {
    byte[] buffer = new byte[1024];
    int i = in.read(buffer);
    while (i > 0)
    {
      out.write(buffer, 0, i);
      i = in.read(buffer);
    }
  }

  public static String getStackTrace(final int depth)
  {
    final StringBuilder b = new StringBuilder();
    final StackTraceElement[] st = Thread.currentThread().getStackTrace();
    final int min = 3;
    final int max = Math.min(min + depth, st.length - 1);
    for (final StackTraceElement e : Arrays.copyOfRange(st, min, max))
    {
      b.append(e.toString());
      b.append("\n");
    }
    return b.toString();
  }
}
