package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class HibiyaParser extends FeedParser
{
  @Override @NonNull
  public PutDataMapRequest parseStream(@NonNull final String dataName, @NonNull final InputStream srcStream) throws IOException
  {
    final PutDataMapRequest result = PutDataMapRequest.create(Const.DATA_PATH);
    final BufferedReader src = new BufferedReader(new InputStreamReader(new BufferedInputStream(srcStream)));
    if (null == find(src, "table summary=\"平日の時刻表\"") || null == find(src, "</tr>")) {
      Logger.L("Argh, data not at expected format for " + dataName);
      return result;
    }
    final Scanner srcTable = new Scanner(find(src, "</table>"));
    final ArrayList<DataMap> buildData = new ArrayList<>();
    srcTable.useDelimiter("(\\s|<|>)+");
    int hour = -1;
    int minute = -1;
    boolean 始発 = false;
    while (srcTable.hasNext()) {
      final String p = srcTable.next();
      if (p.startsWith("class=\"hour\""))
        hour = srcTable.nextInt();
      else if ("class=\"item02\"".equals(p))
        始発 = true;
      else if ("class=\"info02\"".equals(p))
        minute = srcTable.nextInt();
      else if (p.startsWith("/p")) {
        final DataMap departure = new DataMap();
        departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
        departure.putBoolean(Const.DATA_KEY_始発, 始発);
        buildData.add(departure);
        始発 = false;
      }
    }
    result.getDataMap().putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
