package com.j.jface.face;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DataStore
{
  private final HashMap<String, ArrayList<Departure>> mDepartures = new HashMap<>();
  private final HashMap<String, String> mGenericData = new HashMap<>();

  public void putGenericData(@NonNull final String key, @NonNull final String data) {
    mGenericData.put(key, data);
  }

  @NonNull public String getGenericData(@NonNull final String key) {
    return mGenericData.get(key);
  }

  public void putDepartureList(final String dataName, final ArrayList<DataMap> departureList)
  {
    final ArrayList<Departure> departures = new ArrayList<>();
    for (final DataMap map : departureList)
      departures.add(new Departure(map.getInt(Const.DATA_KEY_DEPTIME),
                                   map.getString(Const.DATA_KEY_EXTRA)));
    Collections.sort(departures);
    mDepartures.put(dataName, departures);
  }

  @Nullable public Departure findClosestDeparture(@NonNull final String key, @NonNull final Time time)
  {
    return findClosestDeparture(key, time.hour * 3600 + time.minute * 60);
  }

  @Nullable public Departure findClosestDeparture(@NonNull final String key, @NonNull final int time) {
    final int secs = time % 86400;
    final ArrayList<Departure> deps = mDepartures.get(key);
    if (null == deps) return null;
    Departure nextDeparture = null;
    for (final Departure d : deps)
      if (d.time >= secs) {
        nextDeparture = d;
        break;
      }
    if (null == nextDeparture) nextDeparture = deps.get(0);
    if (nextDeparture.time > secs + 30 * 60) return null; // More than 30 minutes in the future : don't display anything
    return nextDeparture;
  }

  @Nullable public Triplet<Departure> findNextDepartures(@NonNull final String key, @NonNull final Time time) {
    final Departure first = findClosestDeparture(key, time);
    if (null == first) return null;
    final Departure second = findClosestDeparture(key, first.time + 1);
    if (null == second) return new Triplet<>(first, null, null);
    final Departure third = findClosestDeparture(key, second.time + 1);
    return new Triplet<>(first, second, third);
  }
}
