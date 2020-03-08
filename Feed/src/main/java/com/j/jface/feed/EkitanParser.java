package com.j.jface.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

public class EkitanParser extends FeedParser
{
  @Override @NonNull
  public DataMap parseStream(@NonNull final String dataName, @NonNull final BufferedInputStream srcStream, @Nullable final Object arg) throws IOException, ParseException
  {
    final boolean skipFirstTab = Integer.valueOf(2).equals(arg);
    if (!skipFirstTab && !Integer.valueOf(1).equals(arg))
      throw new IllegalArgumentException("EkitanParser : arg must be an Integer, 1 or 2 depending on the desired tab on the page");
    final DataMap result = new DataMap();
    final BufferedReader src = new BufferedReader(new InputStreamReader(srcStream));

    if (skipFirstTab) find(src, "search-result-body");
    final String contents = find(src, "search-result-body");
    if (null == contents) throw new ParseException("EkitanParser : can't parse page " + dataName, 0);

    final ArrayList<DataMap> buildData = new ArrayList<>();
    while (true) {
      if (null == find(src, "data-tr-type=\"")) break;
      final String type = find(src, "\"");
      find(src, "data-departure=\"");
      final String time = find(src, "\""); // Format is HHMM
      final int t = Integer.parseInt(time);
      final int hour = t / 100;
      final int minute = t % 100;

      final String mark = type.startsWith("各") || type.startsWith("普") ? "" : type;
      final DataMap departure = new DataMap();
      departure.putInt(Const.DATA_KEY_DEPTIME, hour * 3600 + minute * 60);
      departure.putString(Const.DATA_KEY_EXTRA, mark);
      buildData.add(departure);
    }

    result.putDataMapArrayList(Const.DATA_KEY_DEPLIST, buildData);
    return result;
  }
}
