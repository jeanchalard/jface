package com.j.jface.face;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.util.ArrayList;

public class DataStore
{
  public void putGenericData(@NonNull final String key, @NonNull final String data) {
    Log.e("\033[31mPUT\033[0m " + key, data);
  }

  public void putDepartureList(final String dataName, final ArrayList<DataMap> departureList)
  {
    Log.e("\033[32mDATA\033[0m", dataName);
    for (final DataMap map : departureList) {
      final Departure dep = new Departure(map.getInt(Const.DATA_KEY_DEPTIME),
                                          map.getBoolean(Const.DATA_KEY_始発));
      Log.e("\033[31mDEP\033[0m", dep.toString());
    }
  }
}
