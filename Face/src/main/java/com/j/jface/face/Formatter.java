package com.j.jface.face;

public class Formatter
{
  public static CharSequence format2Digits(final StringBuilder buffer, final int n)
  {
    buffer.setLength(2);
    buffer.setCharAt(0, (char) ('0' + n / 10));
    buffer.setCharAt(1, (char) ('0' + n % 10));
    return buffer;
  }

  public static int formatBorder(final char[] buffer, final Draw.Params p)
  {
    int i = -1;
    buffer[++i] = (char) ('0' + (p.time.year / 1000) % 10);
    buffer[++i] = (char) ('0' + (p.time.year / 100) % 10);
    buffer[++i] = (char) ('0' + (p.time.year / 10) % 10);
    buffer[++i] = (char) ('0' + p.time.year % 10);
    buffer[++i] = '/';
    final int month = p.time.month + 1;
    buffer[++i] = (char) ('0' + (month / 10) % 10);
    buffer[++i] = (char) ('0' + month % 10);
    buffer[++i] = '/';
    buffer[++i] = (char) ('0' + (p.time.monthDay / 10) % 10);
    buffer[++i] = (char) ('0' + p.time.monthDay % 10);
    buffer[++i] = ' ';
    buffer[++i] = '-';
    buffer[++i] = ' ';
    if (p.pressure > 1000)
      buffer[++i] = (char) ('0' + (int) (p.pressure / 1000));
    buffer[++i] = (char) ('0' + (int) ((p.pressure / 100) % 10));
    buffer[++i] = (char) ('0' + (int) ((p.pressure / 10) % 10));
    buffer[++i] = (char) ('0' + (int) (p.pressure % 10));
    buffer[++i] = '.';
    buffer[++i] = (char) ('0' + Math.round(p.pressure * 10) % 10);
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
