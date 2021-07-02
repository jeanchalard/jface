package com.j.jface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;

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
    final int[] r = Arrays.copyOf(a1, a1.length + a2.length);
    System.arraycopy(a2, 0, r, a1.length, a2.length);
    return r;
  }

  public static @NonNull <T> T[] concat(T[] a1, T[] a2)
  {
    final T[] r = Arrays.copyOf(a1, a1.length + a2.length);
    System.arraycopy(a2, 0, r, a1.length, a2.length);
    return r;
  }

  // String.join doesn't exist until later releases
  public static @NonNull String join(@Nullable final String[] array, @NonNull final String separator) {
    if (null == array || 0 == array.length) return "";
    final StringBuilder s = new StringBuilder(array[0]);
    for (int i = 1; i < array.length; ++i)
    {
      s.append(separator);
      s.append(array[i]);
    }
    return s.toString();
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

  public static File copy(@NonNull final InputStream in, @NonNull final File out) throws IOException
  {
    copy(in, new FileOutputStream(out));
    return out;
  }
  public static File copy(@NonNull final File in, @NonNull final File out) throws IOException { return copy(new FileInputStream(in), out); }

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

  public static String nowStr() // Using only old Java classes so it works on old OSes
  {
    final GregorianCalendar date = new GregorianCalendar();
    return String.format(Locale.JAPAN, "%02d-%02d:%02d%02d", date.get(MONTH) + 1, date.get(DAY_OF_MONTH), date.get(HOUR_OF_DAY), date.get(MINUTE));
  }
}
