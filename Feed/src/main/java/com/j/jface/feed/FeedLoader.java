package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FeedLoader
{
  static ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

  private static class DataSource {
    public final String name;
    public final String url;
    public final Class<? extends FeedParser> parser;
    public DataSource(final String n, final String u, final Class<? extends FeedParser> p) { name = n; url = u; parser = p; }
  }
  private static final DataSource[] DATA_SOURCES = {
   new DataSource(Const.日比谷線_北千住_平日,
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/index.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_北千住_休日,
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/holiday.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_六本木_平日,
    "http://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/index.html", HibiyaParser.class),
   new DataSource(Const.日比谷線_六本木_休日,
    "http://www.tokyometro.jp/station/roppongi/timetable/hibiya/b/holiday.html", HibiyaParser.class),
   new DataSource(Const.京成線_上野方面_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=1", KeiseiParser.class),
   new DataSource(Const.京成線_上野方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=49&dw=1&slCode=254-4&d=1", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=35&dw=0&slCode=254-4&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=50&dw=1&slCode=254-4&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_日暮里_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=44&dw=0&slCode=254-1&d=2", KeiseiParser.class),
   new DataSource(Const.京成線_日暮里_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?dir=52&dw=1&slCode=254-1&d=2", KeiseiParser.class)
  };

  public static void startAllLoads(@NonNull final Client client) {
    for (final DataSource ds : DATA_SOURCES)
      startLoadDataSource(ds, client);
  }

  private static long getLastSuccessfulUpdateDate(@NonNull final Client client, @NonNull final String path)
  {
    final DataMap lastStatusData = client.getData(path);
    if (null == lastStatusData) return 0;
    final Long lastStatus = lastStatusData.get(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE);
    if (null == lastStatus) return 0;
    return lastStatus;
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final Client client) {
    executor.execute(new Runnable() { public void run()
    {
      final String dataPath = Const.DATA_PATH + "/" + ds.name;
      final String statusDataPath = dataPath + "_status";
      final long lastSuccessfulUpdateDate = getLastSuccessfulUpdateDate(client, statusDataPath);
      final long now = System.currentTimeMillis();
      if (lastSuccessfulUpdateDate + Const.UPDATE_DELAY_MILLIS > now)
      {
        Logger.L("Update for " + ds.name + " is " +
         ((lastSuccessfulUpdateDate + Const.UPDATE_DELAY_MILLIS - now) / 3600000) + " hours away");
        return;
      }
      final DataMap statusData = new DataMap();
      statusData.putLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE, lastSuccessfulUpdateDate);
      try
      {
        final URL url = new URL(ds.url);
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        if (url.getAuthority().startsWith("keisei"))
          urlConnection.addRequestProperty("User-Agent", "Mozilla");
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.parser.newInstance();
        final DataMap data = parser.parseStream(ds.name, in);
        Logger.L("Updated data for " + ds.name);
        client.putData(dataPath, data);
        statusData.putLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE, System.currentTimeMillis());
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Success");
      } catch (@NonNull InstantiationException | IllegalAccessException | IOException | RuntimeException e) {
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Failure ; " + e.getMessage());
      }
      statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
      client.putData(statusDataPath, statusData);
    }});
  }
}
