package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.wear.Wear;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FeedLoader
{
  private static final ThreadPoolExecutor executor =
   new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

  public static void startAllLoads(@NonNull final Wear wear)
  {
    for (final DataSource ds : DataSource.ALL_SOURCES)
      startLoadDataSource(ds, wear);
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final Wear wear)
  {
    executor.execute(() ->
    {
      final String dataPath = Const.DATA_PATH + "/" + ds.name;
      final String statusDataPath = dataPath + Const.DATA_PATH_SUFFIX_STATUS;
      final DataMap statusData = wear.getDataSynchronously(statusDataPath);
      final long lastSuccessfulUpdateDate = statusData.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE);
      final long now = System.currentTimeMillis();
      if (lastSuccessfulUpdateDate + Const.UPDATE_DELAY_MILLIS > now)
      {
        statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
        wear.putDataLocally(statusDataPath, statusData);
        return;
      }
      try
      {
        final URL url = new URL(ds.url);
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        if (url.getAuthority().startsWith("keisei"))
          urlConnection.addRequestProperty("User-Agent", "Mozilla");
        final BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.parser.newInstance();
        final DataMap data = parser.parseStream(ds.name, in);
        wear.putDataLocally(dataPath, data);
        statusData.putLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE, System.currentTimeMillis());
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Success");
      }
      catch (@NonNull InstantiationException | IllegalAccessException | IOException | RuntimeException e)
      {
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Failure ; " + e.getMessage());
      }
      statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
      wear.putDataLocally(statusDataPath, statusData);
    });
  }
}
