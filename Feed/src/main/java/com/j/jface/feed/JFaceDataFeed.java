package com.j.jface.feed;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;

public class JFaceDataFeed
{
  @NonNull private final Activity mA;
  @NonNull private final EditText mDataEdit;
  @NonNull private final TextView mLog;
  @NonNull private final Client mClient;

  public JFaceDataFeed(@NonNull final Activity a)
  {
    mA = a;
    mA.setContentView(R.layout.activity_digital_watch_face_config);
    mDataEdit = (EditText) mA.findViewById(R.id.dataItem);
    mLog = (TextView) mA.findViewById(R.id.log);
    mClient = new Client(a);
    startGeofenceService(mA);
    retrieveStatus(mClient);
  }

  private void startGeofenceService(final Activity activity)
  {
    final Intent i = new Intent(activity, GeofenceTransitionReceiverService.class);
    i.setAction(GeofenceTransitionReceiver.ACTION_MANUAL_START);
    activity.startService(i);
  }

  private Client.GetDataCallback showDataCallback()
  {
    return new Client.GetDataCallback() { public void run(@NonNull final String path, @NonNull final DataMap dataMap) {
      mA.runOnUiThread(new Runnable() { @Override public void run() {
        mLog.append(
         path.substring(Const.DATA_PATH.length() + 1, path.length() - Const.DATA_PATH_SUFFIX_STATUS.length()));
        mLog.append("\n");
        final long lastUpdate = dataMap.getLong(Const.DATA_KEY_STATUS_UPDATE_DATE);
        if (0 == lastUpdate)
        {
          mLog.append(" Never updated\n");
          return;
        }
        final Time t = new Time();
        t.set(lastUpdate);
        final String status = dataMap.getString(Const.DATA_KEY_LAST_STATUS);
        mLog.append(" Updated on " + t.format("%Y/%m/%d %H:%M:%S"));
        mLog.append("\n");
        mLog.append(" Status : " + status);
        mLog.append("\n");
        if ("Success".equals(status)) return;
        t.set(dataMap.getLong(Const.DATA_KEY_SUCCESSFUL_UPDATE_DATE));

        mLog.append(" Last successful update on " + t.format("%Y/%m/%d %H:%M:%S"));
      }});}};
  }

  private void retrieveStatus(final Client client)
  {
    for (final DataSource ds : DataSource.ALL_SOURCES)
      client.getData(Const.DATA_PATH + "/" + ds.name + Const.DATA_PATH_SUFFIX_STATUS, showDataCallback());
  }

  public void onClickSet(@NonNull final View button)
  {
    mClient.putData(Const.DATA_KEY_ADHOC, Const.DATA_KEY_ADHOC, mDataEdit.getText().toString());
  }

  public void load()
  {
    Logger.L("Start loading");
    FeedLoader.startAllLoads(mClient);
  }

  public void clear()
  {
    mClient.deleteAllData();
  }
}
