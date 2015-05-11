package com.j.jface.face;

import com.j.jface.Departure;

public class NextDepartures
{
  final String key;
  final Departure first;
  final Departure second;
  final Departure third;
  public NextDepartures(final String k, final Departure f, final Departure s, final Departure t) {
    key = k;
    first = f;
    second = s;
    third = t;
  }
}
