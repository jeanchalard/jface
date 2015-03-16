package com.j.jface.face;

public class Departure
{
  final int mTime;
  final boolean m始発;
  public Departure(final int time, final boolean 始発) {
    mTime = time;
    m始発 = 始発;
  }
  public String toString() {
    return Integer.toString(mTime / 3600) + ":" + Integer.toString((mTime % 3600) / 60) + (m始発 ? "・" : "");
  }
}
