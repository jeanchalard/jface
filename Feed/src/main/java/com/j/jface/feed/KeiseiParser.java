package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class KeiseiParser extends FeedParser
{
  @Override @NonNull
  public DataMap parseStream(@NonNull final String dataName, @NonNull final InputStream srcStream) throws IOException
  {
    final DataMap result = new DataMap();
    final BufferedReader src = new BufferedReader(new InputStreamReader(new BufferedInputStream(srcStream), "Shift_JIS"));

    if (null == find(src, "■京成本線") || null == find(src, "</tr>")) {
      Logger.L("Argh, data not at expected format for " + dataName);
      return result;
    }
    final Scanner srcTable = new Scanner(find(src, "</table>"));
    final ArrayList<DataMap> buildData = new ArrayList<>();
    srcTable.useDelimiter("(\\s|<|>|&)+");
    int hour = -1;
    int minute = -1;
    boolean 快速 = false;
    while (srcTable.hasNext()) {
      final String p = srcTable.next();
      if (p.startsWith("class=\"side01\"") || p.startsWith("class=\"side02\""))
        hour = srcTable.nextInt();
      else if ("快速".equals(p))
        快速 = true;
      else if ("br".equals(p) && "/".equals(srcTable.next()))
        minute = srcTable.nextInt();
      else if (p.startsWith("/div")) {
        final DataMap departure = new DataMap();
        departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
        departure.putString(Const.DATA_KEY_EXTRA, 快速 ? "快" : "");
        buildData.add(departure);
        快速 = false;
      }
    }
    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
