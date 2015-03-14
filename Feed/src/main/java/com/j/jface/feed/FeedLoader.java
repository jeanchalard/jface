package com.j.jface.feed;

import android.support.annotation.NonNull;

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
   new DataSource("日比谷線・北千住・平日",
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/index.html", HibiyaParser.class),
   new DataSource("日比谷線・北千住・休日",
    "http://www.tokyometro.jp/station/kita-senju/timetable/hibiya/a/holiday.html", HibiyaParser.class)
  };

  public static void startAllLoads(@NonNull final UpdateHandler handler) {
    for (final DataSource ds : DATA_SOURCES)
      startLoadDataSource(ds, handler);
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final UpdateHandler client) {
    URL u = null;
    try { u = new URL(ds.url); } catch (MalformedURLException e) {} // Can't happen because we know the URL is valid
    final URL url = u;
    executor.execute(new Runnable() { public void run()
    {
      HttpURLConnection urlConnection = null;
      try
      {
        urlConnection = (HttpURLConnection)url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.parser.newInstance();
        parser.parseStream(in);
      } catch (@NonNull InstantiationException | IllegalAccessException e) {} // Nopes never happens
      catch (@NonNull IOException e)
      {
        // TODO : Must reschedule
      }
    }});
  }
}
