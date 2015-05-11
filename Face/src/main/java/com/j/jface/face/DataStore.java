package com.j.jface.face;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.Departure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DataStore
{
  private final HashMap<String, ArrayList<Departure>> mDepartures = new HashMap<>();
  private final HashMap<String, Boolean> mLocationStatuses = new HashMap<>();
  private final HashMap<String, String> mGenericData = new HashMap<>();

  public void putGenericData(@NonNull final String key, @NonNull final String data)
  {
    mGenericData.put(key, data);
  }

  @NonNull public String getGenericData(@NonNull final String key) {
    return mGenericData.get(key);
  }

  public void putLocationStatus(final String fenceName, final boolean isInside)
  {
    mLocationStatuses.put(fenceName, isInside);
  }

  @NonNull private ArrayList<Departure> relink(@NonNull final ArrayList<Departure> src)
  {
    final int lastIndex = src.size() - 1;
    final ArrayList<Departure> dst = new ArrayList<>(lastIndex + 1);
    for (int i = 0; i <= lastIndex; ++i) dst.add(null);
    Departure lastDep = src.get(lastIndex);
    dst.set(lastIndex, lastDep);
    for (int i = lastIndex - 1; i >= 0; --i)
    {
      final Departure d = src.get(i);
      lastDep = new Departure(d.time, d.extra, d.key, lastDep);
      dst.set(i, lastDep);
    }
    return dst;
  }

  public void putDepartureList(final String dataName, final ArrayList<DataMap> departureList)
  {
    final ArrayList<Departure> tmpDeps = new ArrayList<>();
    for (final DataMap map : departureList)
      tmpDeps.add(new Departure(map.getInt(Const.DATA_KEY_DEPTIME),
                                   map.getString(Const.DATA_KEY_EXTRA),
                                   dataName, null));
    Collections.sort(tmpDeps);
    final ArrayList<Departure> a = relink(tmpDeps);
    mDepartures.put(dataName, a);
  }

  @Nullable public Departure findClosestDeparture(@NonNull final String key, @NonNull final Time time)
  {
    return findClosestDeparture(key, time.hour * 3600 + time.minute * 60);
  }

  @Nullable public Departure findClosestDeparture(@NonNull final String key, @NonNull final int time)
  {
    final int secsSinceMidnight = time % 86400;
    final ArrayList<Departure> deps = mDepartures.get(key);
    if (null == deps) return null;
    Departure nextDeparture = null;
    final int len = deps.size();
    // Loop with an index to avoid iterator allocation
    for (int i = 0; i < len; ++i)
    {
      final Departure d = deps.get(i);
      if (d.time >= secsSinceMidnight)
      {
        nextDeparture = d;
        break;
      }
    }
    if (null == nextDeparture) nextDeparture = deps.get(0);
    if (nextDeparture.time > secsSinceMidnight + 30 * 60) return null; // More than 30 minutes in the future : don't display anything
    return nextDeparture;
  }

  public Boolean isWithinFence(@NonNull final String fenceName)
  {
    return mLocationStatuses.get(fenceName);
  }
}
