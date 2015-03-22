package com.j.jface.face;

import android.support.annotation.NonNull;
import android.text.format.Time;

public class Formatter
{
  public static CharSequence format2Digits(final StringBuilder buffer, final int n)
  {
    buffer.setLength(2);
    buffer.setCharAt(0, (char) ('0' + n / 10));
    buffer.setCharAt(1, (char) ('0' + n % 10));
    return buffer;
  }

  public static int formatBorder(final char[] buffer, @NonNull final Time time, final float pressure)
  {
    int i = -1;
    buffer[++i] = (char) ('0' + (time.year / 1000) % 10);
    buffer[++i] = (char) ('0' + (time.year / 100) % 10);
    buffer[++i] = (char) ('0' + (time.year / 10) % 10);
    buffer[++i] = (char) ('0' + time.year % 10);
    buffer[++i] = '/';
    final int month = time.month + 1;
    buffer[++i] = (char) ('0' + (month / 10) % 10);
    buffer[++i] = (char) ('0' + month % 10);
    buffer[++i] = '/';
    buffer[++i] = (char) ('0' + (time.monthDay / 10) % 10);
    buffer[++i] = (char) ('0' + time.monthDay % 10);
    buffer[++i] = ' ';
    buffer[++i] = '-';
    buffer[++i] = ' ';
    if (pressure > 1000)
      buffer[++i] = (char) ('0' + (int) (pressure / 1000));
    buffer[++i] = (char) ('0' + (int) ((pressure / 100) % 10));
    buffer[++i] = (char) ('0' + (int) ((pressure / 10) % 10));
    buffer[++i] = (char) ('0' + (int) (pressure % 10));
    buffer[++i] = '.';
    buffer[++i] = (char) ('0' + Math.round(pressure * 10) % 10);
    buffer[++i] = 'h';
    buffer[++i] = 'P';
    buffer[++i] = 'a';
    return i;
  }

  public static CharSequence formatDeparture(final StringBuilder buffer, final Departure dep, final int offset)
  {
    buffer.setLength(offset + 5);
    int i = offset - 1;
    buffer.setCharAt(++i, (char) ('0' + dep.time / 36000));
    buffer.setCharAt(++i, (char) ('0' + (dep.time / 3600) % 10));
    buffer.setCharAt(++i, ':');
    buffer.setCharAt(++i, (char) ('0' + (dep.time % 3600) / 600));
    buffer.setCharAt(++i, (char) ('0' + (dep.time % 600) / 60));
    buffer.append(dep.extra);
    return buffer;
  }
}
