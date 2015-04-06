package com.j.jface.feed;

import android.app.Activity;
import android.location.Location;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.j.jface.Const;
import com.j.jface.R;
import com.j.jface.feed.actions.PutDataAction;

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
    Logger.setLogger(this);
  }

  public void Log(@NonNull final String s)
  {
    mA.runOnUiThread(new Runnable()
    {
      public void run()
      {
        mLog.append(s);
        mLog.append("\n");
      }
    });
  }

  public void onClickSet(@NonNull final View button)
  {
    setValue(mDataEdit.getText().toString());
  }

  private void setValue(@NonNull final String value)
  {
    mClient.enqueue(new PutDataAction(Const.DATA_KEY_ADHOC, Const.DATA_KEY_ADHOC, value));
  }

  public void load()
  {
    Logger.L("Start loading");
    FeedLoader.startAllLoads(mClient);
  }

  public void onConnected(@NonNull final GoogleApiClient client)
  {
    final Location l = LocationServices.FusedLocationApi.getLastLocation(client);
  }

  // Save for the future, in case we need it
//  private void sendConfigUpdateMessage(final String configKey, final String msg)
//  {
//    final DataMap config = new DataMap();
//    config.putString(configKey, msg);
//    final byte[] rawData = config.toByteArray();
//    Wearable.MessageApi.sendMessage(mClient, PEER_ID, DATA_PATH, rawData);
//  }
}
