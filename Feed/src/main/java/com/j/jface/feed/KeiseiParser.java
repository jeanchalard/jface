package com.j.jface.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

public class KeiseiParser extends FeedParser
{
  @Override @NonNull
  public DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream srcStream, @Nullable final Object arg) throws IOException
  {
    if (null != arg) throw new IllegalArgumentException("KeiseiParser : arg must be null.");
    final DataMap result = new DataMap();
    final BufferedReader src = new BufferedReader(new InputStreamReader(new BufferedInputStream(srcStream), "UTF-8"));

    if (null == find(src, "class=\"name\">") || null == find(src, "</tr>"))
      return result;
    final Scanner srcTable = new Scanner(find(src, "</table>"));
    final ArrayList<DataMap> buildData = new ArrayList<>();
    srcTable.useDelimiter("(\\s|<|>|&)+");
    int hour = -1;
    int minute = -1;
    boolean 快速 = false;
    boolean willStop = true;
    while (srcTable.hasNext())
    {
      final String p = srcTable.next();
      if (p.startsWith("class=\"side01\"") || p.startsWith("class=\"side02\""))
        hour = srcTable.nextInt();
      else if ("快速".equals(p))
        快速 = true;
      else if ("br".equals(p) && "/".equals(srcTable.next()))
        minute = srcTable.nextInt();
      else if ("ス".equals(p) || "イ".equals(p) || "ア特".equals(p) || "快特".equals(p) || "特急".equals(p))
        willStop = false;
      else if (p.startsWith("/div"))
      {
        if (willStop)
        {
          final DataMap departure = new DataMap();
          departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
          departure.putString(Const.DATA_KEY_EXTRA, 快速 ? "快" : "");
          buildData.add(departure);
        }
        快速 = false;
        willStop = true;
      }
    }
    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
