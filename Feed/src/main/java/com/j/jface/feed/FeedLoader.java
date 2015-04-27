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

  public static void startAllLoads(@NonNull final Client client) {
    for (final DataSource ds : DataSource.ALL_SOURCES)
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
      final String statusDataPath = dataPath + Const.DATA_PATH_SUFFIX_STATUS;
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
