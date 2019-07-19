package com.j.jface.feed;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class HibiyaParser extends FeedParser
{
  @Override @NonNull
  public DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream srcStream) throws IOException, ParseException
  {
    final DataMap result = new DataMap();
    final BufferedReader src = new BufferedReader(new InputStreamReader(srcStream));
    if (null == find(src, "table class=\"v2_table "))
      throw new ParseException("HibiyaParser : can't parse page " + dataName, 0);
    final Scanner srcTable = new Scanner(src);
    final ArrayList<DataMap> buildData = new ArrayList<>();
    srcTable.useDelimiter("(<|>)+");
    int hour = -1;
    int minute = -1;
    boolean 始発 = false;
    while (srcTable.hasNext()) {
      final String p = srcTable.next();
      if ("th".equals(p))
        hour = srcTable.nextInt();
      else if ("span class=\"v2_tableTimeTxt\"".equals(p) && srcTable.next().contains("●"))
        始発 = true;
      else if (p.startsWith("a href=\"/station/timetable.html"))
        minute = srcTable.nextInt();
      else if ("/table".equals(p))
        break;
      else if (p.startsWith("/li"))
      {
        final DataMap departure = new DataMap();
        departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
        departure.putString(Const.DATA_KEY_EXTRA, 始発 ? "始" : "");
        buildData.add(departure);
        始発 = false;
      }
    }
    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
