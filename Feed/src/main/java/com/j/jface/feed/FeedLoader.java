package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.j.jface.Const;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FeedLoader
{
  static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

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
    "http://keisei.ekitan.com/norikae/pc/T5?uid=69927&dir=35&USR=PC&dw=0&slCode=254-4&d=1&rsf=%90%E7%8F%5A%91%E5%8B%B4", KeiseiParser.class),
   new DataSource(Const.京成線_上野方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?uid=69927&dir=49&USR=PC&dw=1&slCode=254-4&d=1&rsf=%90%E7%8F%5A%91%E5%8B%B4", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_平日,
    "http://keisei.ekitan.com/norikae/pc/T5?uid=69927&dir=35&USR=PC&dw=0&slCode=254-4&d=2&rsf=%90%E7%8F%5A%91%E5%8B%B4", KeiseiParser.class),
   new DataSource(Const.京成線_成田方面_休日,
    "http://keisei.ekitan.com/norikae/pc/T5?uid=69927&dir=50&USR=PC&dw=1&slCode=254-4&d=2&rsf=%90%E7%8F%5A%91%E5%8B%B4", KeiseiParser.class)
  };

  public static void startAllLoads(@NonNull final UpdateHandler handler) {
    for (final DataSource ds : DATA_SOURCES)
      startLoadDataSource(ds, handler);
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final UpdateHandler handler) {
    final URL url;
    try { url = new URL(ds.url); } catch (MalformedURLException e) { return; } // Can't happen because the URL is valid
    executor.execute(new Runnable() { public void run()
    {
      HttpURLConnection urlConnection = null;
      try
      {
        urlConnection = (HttpURLConnection)url.openConnection();
        if (url.getAuthority().startsWith("keisei"))
          urlConnection.addRequestProperty("User-Agent", "Mozilla");
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.parser.newInstance();
        handler.handleUpdate(ds.name, parser.parseStream(ds.name, in));
      } catch (@NonNull InstantiationException | IllegalAccessException e) {} // Nopes never happens
      catch (@NonNull IOException e)
      {
        // TODO : Must reschedule
      }
    }});
  }
}
