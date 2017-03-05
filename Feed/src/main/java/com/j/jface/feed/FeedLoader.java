package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.client.Client;

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
  private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

  public static void startAllLoads(@NonNull final Client client) {
    for (final DataSource ds : DataSource.ALL_SOURCES)
      startLoadDataSource(ds, client);
  }

  private static DataMap getStatusData(@NonNull final Client client, @NonNull final String path)
  {
    final DataMap data = client.getData(path);
    return null == data ? new DataMap() : data;
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final Client client) {
    executor.execute(new Runnable() { public void run()
    {
      final String dataPath = Const.DATA_PATH + "/" + ds.name;
      final String statusDataPath = dataPath + Const.DATA_PATH_SUFFIX_STATUS;
      final DataMap statusData = getStatusData(client, statusDataPath);
      final long lastSuccessfulUpdateDate = statusData.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE);
      final long now = System.currentTimeMillis();
      if (lastSuccessfulUpdateDate + Const.UPDATE_DELAY_MILLIS > now)
      {
        statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
        client.putData(statusDataPath, statusData);
        return;
      }
      try
      {
        final URL url = new URL(ds.url);
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        if (url.getAuthority().startsWith("keisei"))
          urlConnection.addRequestProperty("User-Agent", "Mozilla");
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.parser.newInstance();
        final DataMap data = parser.parseStream(ds.name, in);
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
