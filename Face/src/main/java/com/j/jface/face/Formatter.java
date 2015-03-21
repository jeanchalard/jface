package com.j.jface.face;

public class Formatter
{
  private final StringBuffer buffer = new StringBuffer(256);

  public Formatter() {}

  public CharSequence format2Digits(final int n)
  {
    buffer.setLength(2);
    buffer.setCharAt(0, (char)('0' + n / 10));
    buffer.setCharAt(1, (char)('0' + n % 10));
    return buffer;
  }

  public CharSequence formatBorder(final Draw.Params p)
  {
    buffer.setLength(25);
    int i = -1;
    buffer.setCharAt(++i, (char)('0' + (p.time.year / 1000) % 10));
    buffer.setCharAt(++i, (char)('0' + (p.time.year / 100) % 10));
    buffer.setCharAt(++i, (char)('0' + (p.time.year / 10) % 10));
    buffer.setCharAt(++i, (char)('0' + p.time.year % 10));
    buffer.setCharAt(++i, '/');
    final int month = p.time.month + 1;
    buffer.setCharAt(++i, (char)('0' + (month / 10) % 10));
    buffer.setCharAt(++i, (char)('0' + month % 10));
    buffer.setCharAt(++i, '/');
    buffer.setCharAt(++i, (char)('0' + (p.time.monthDay / 10) % 10));
    buffer.setCharAt(++i, (char)('0' + p.time.monthDay % 10));
    buffer.setCharAt(++i, ' ');
    buffer.setCharAt(++i, '-');
    buffer.setCharAt(++i, ' ');
    if (p.pressure > 1000)
      buffer.setCharAt(++i, (char)('0' + (int)(p.pressure / 1000)));
    buffer.setCharAt(++i, (char)('0' + (int)((p.pressure / 100) % 10)));
    buffer.setCharAt(++i, (char)('0' + (int)((p.pressure / 10) % 10)));
    buffer.setCharAt(++i, (char)('0' + (int)(p.pressure % 10)));
    buffer.setCharAt(++i, '.');
    buffer.setCharAt(++i, (char)('0' + Math.round(p.pressure * 10) % 10));
    buffer.setCharAt(++i, 'h');
    buffer.setCharAt(++i, 'P');
    buffer.setCharAt(++i, 'a');
    buffer.setLength(i);
    return buffer;
  }

  public CharSequence formatDeparture(final Departure dep, final int offset)
  {
    buffer.setLength(offset + 5);
    int i = offset - 1;
    buffer.setCharAt(++i, (char) ('0' + dep.time / 36000));
    buffer.setCharAt(++i, (char) ('0' + (dep.time / 3600) % 10));
    buffer.setCharAt(++i, ':');
    buffer.setCharAt(++i, (char) ('0' + (dep.time % 3600) / 600));
    buffer.setCharAt(++i, (char)('0' + (dep.time % 600) / 60));
    buffer.append(dep.extra);
    return buffer;
  }

  // Whoa this is really crazy
  public CharSequence append(final String s)
  {
    buffer.append(s);
    return buffer;
  }
}
