package com.j.jface.feed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.action.InformUserAction;
import com.j.jface.wear.Wear;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public class FeedLoader
{
  private static final ThreadPoolExecutor executor =
   new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

  public static void startAllLoads(@NonNull final Wear wear, @NonNull String reason)
  {
    final Context context = wear.getContext();
    LocalLog.INSTANCE.log(context, "Starting all loads : " + reason);
    final Network network = context.getSystemService(ConnectivityManager.class).getActiveNetwork();
    if (null == network) {
      LocalLog.INSTANCE.log(context, "No active network – skipping load.");
      return;
    }
    for (final DataSource ds : DataSource.ALL_SOURCES)
      startLoadDataSource(ds, wear);
  }

  private static StackTraceElement findRelevantStackTraceElement(@NonNull final StackTraceElement[] stack)
  {
    for (final StackTraceElement e : stack)
      if (e.getClassName().startsWith("com.j.jface")) return e;
    return stack[0];
  }

  private static void startLoadDataSource(@NonNull final DataSource ds, @NonNull final Wear wear)
  {
    executor.execute(() ->
    {
      final String dataPath = Const.DATA_PATH + "/" + ds.getName();
      final String statusDataPath = dataPath + Const.DATA_PATH_SUFFIX_STATUS;
      final DataMap statusData = wear.getDataSynchronously(statusDataPath);
      final long lastSuccessfulUpdateDate = statusData.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE);
      final long now = System.currentTimeMillis();
      if (lastSuccessfulUpdateDate + Const.UPDATE_DELAY_MILLIS > now)
      {
        statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
        LocalLog.INSTANCE.log(wear.getContext(), "Update is recent – skipping load.");
        wear.putDataLocally(statusDataPath, statusData);
        return;
      }
      try
      {
        final URL url = new URL(ds.getUrl());
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        if (url.getAuthority().startsWith("keisei"))
          urlConnection.addRequestProperty("User-Agent", "Mozilla");
        urlConnection.setInstanceFollowRedirects(true);
        int code = urlConnection.getResponseCode();
        if (2 != code / 100) throw new ParseException("Can't open URL (HTTP" + code + ") : " + url, 0);
        final BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
        final FeedParser parser = ds.getParser().newInstance();
        final DataMap data = parser.parseStream(ds.getName(), in, ds.getArg());
        wear.putDataLocally(dataPath, data);
        statusData.putLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE, System.currentTimeMillis());
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Success");
        LocalLog.INSTANCE.log(wear.getContext(), "Successfully loaded data " + ds.getName());
      }
      catch (@NonNull InstantiationException | IllegalAccessException | IOException | ParseException | RuntimeException e)
      {
        final String details = findRelevantStackTraceElement(e.getStackTrace()).toString() + "\n" + ds.getUrl();
        statusData.putString(Const.DATA_KEY_LAST_STATUS, "Failure ; " + e.getMessage());
        new InformUserAction(wear.getContext(), e.toString(), details,null, null, null).invoke();
      }
      finally
      {
        statusData.putLong(Const.DATA_KEY_STATUS_UPDATE_DATE, System.currentTimeMillis());
        wear.putDataLocally(statusDataPath, statusData);
      }
    });
  }
}
