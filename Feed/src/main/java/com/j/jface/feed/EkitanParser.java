package com.j.jface.feed;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class EkitanParser extends FeedParser
{
  @Override @NonNull
  public DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream srcStream) throws IOException
  {
    final DataMap result = new DataMap();
    final BufferedReader src = new BufferedReader(new InputStreamReader(srcStream));

    // Warning. 駅情報 appears twice in the page, once for each direction. For now we only parse this
    // page for 稲城 towards 新宿 and 本蓮沼 toward 目黒, and as it happens both of these have the relevant
    // direction in first, so it's going to work for our purposes. However we'll need something more
    // sophisticated if we ever need to disambiguate.
    find(src, "駅情報");
    final Scanner srcTable = new Scanner(find(src, "駅情報"));
    final ArrayList<DataMap> buildData = new ArrayList<>();
    srcTable.useDelimiter("(<|>|:)+");
    int hour = -1;
    int minute = -1;
    while (srcTable.hasNext())
    {
      final String p = srcTable.next();
      if (p.startsWith("span class=\"dep-time\""))
      {
        hour = srcTable.nextInt();
        minute = srcTable.nextInt();
      }
      else if (p.startsWith("span class=\"train-type"))
      {
        final String type = srcTable.next().substring(0, 1);
        final String mark = "各".equals(type) || "普".equals(type) ? "" : type;
        final DataMap departure = new DataMap();
        departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
        departure.putString(Const.DATA_KEY_EXTRA, mark);
        buildData.add(departure);
      }
    }
    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
