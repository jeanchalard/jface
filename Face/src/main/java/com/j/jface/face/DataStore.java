package com.j.jface.face;

import android.graphics.Bitmap;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.Departure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DataStore
{
  @NonNull private final HashMap<String, ArrayList<Departure>> mDepartures = new HashMap<>();
  @NonNull private final HashMap<String, Boolean> mLocationStatuses = new HashMap<>();
  @NonNull public String mUserMessage = "";
  @NonNull public int[] mUserMessageColors = new int[0];
  @Nullable public Bitmap mBackground;
  public long mTimeOffset;
  public long mDebugFences;

  public void putLocationStatus(final String fenceName, final boolean isInside)
  {
    mLocationStatuses.put(fenceName, isInside);
  }

  public long currentTimeMillis()
  {
    return System.currentTimeMillis() + mTimeOffset * 1000;
  }

  @NonNull private ArrayList<Departure> relink(@NonNull final ArrayList<Departure> src)
  {
    final int srcSize = src.size();
    final ArrayList<Departure> dst = new ArrayList<>(srcSize + 1);
    for (int i = 0; i <= srcSize; ++i) dst.add(null);
    Departure lastDep = src.get(srcSize - 1);
    lastDep = new Departure(-1, lastDep.extra, lastDep.key, null);
    dst.set(srcSize, lastDep);
    for (int i = srcSize - 1; i >= 0; --i)
    {
      final Departure d = src.get(i);
      lastDep = new Departure(d.time, d.extra, d.key, lastDep);
      dst.set(i, lastDep);
    }
    return dst;
  }

  public void
  putDepartureList(final String dataName, final ArrayList<DataMap> departureList)
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

  @Nullable public Departure findClosestDeparture(@NonNull final String key, final int time)
  {
    final int secsSinceRealMidnight = time % 86400;
    final int secsSinceLogicalMidnight =
     secsSinceRealMidnight < 3 * 3600 ? secsSinceRealMidnight + 86400 : secsSinceRealMidnight;
    final ArrayList<Departure> deps = mDepartures.get(key);
    if (null == deps) return null;
    Departure nextDeparture = null;
    final int len = deps.size();
    // Loop with an index to avoid iterator allocation
    for (int i = 0; i < len; ++i)
    {
      final Departure d = deps.get(i);
      if (d.dTime >= secsSinceLogicalMidnight)
      {
        nextDeparture = d;
        break;
      }
    }
    if (null == nextDeparture) nextDeparture = deps.get(0);
    if (nextDeparture.time > secsSinceRealMidnight + 30 * 60) return null; // More than 30 minutes in the future : don't display anything
    return nextDeparture;
  }

  @Nullable public Departure findPrevDeparture(@NonNull final Departure dep)
  {
    final ArrayList<Departure> deps = mDepartures.get(dep.key);
    final int len = deps.size();
    for (int i = 0; i < len; ++i)
      if (deps.get(i) == dep) return i <= 0 ? null : deps.get(i - 1);
    return null; // This should never happen
  }

  public Boolean isWithinFence(@NonNull final String fenceName)
  {
    if (0 == mDebugFences)
      return mLocationStatuses.get(fenceName);
    for (int i = Const.ALL_FENCE_NAMES.length - 1; i >= 0; --i)
      if (Const.ALL_FENCE_NAMES[i].equals(fenceName))
        return (mDebugFences & (1 << i)) != 0;
    return Boolean.FALSE;
  }
}
