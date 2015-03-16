package com.j.jface.feed;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.j.jface.Const;
import com.j.jface.R;

public class JFaceDataFeed
{
  @NonNull
  private final Activity mA;
  @NonNull
  private final EditText mDataEdit;
  @NonNull
  private final TextView mLog;

  public JFaceDataFeed(@NonNull final Activity a)
  {
    mA = a;
    mA.setContentView(R.layout.activity_digital_watch_face_config);
    mDataEdit = (EditText) mA.findViewById(R.id.dataItem);
    mLog = (TextView) mA.findViewById(R.id.log);
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

  public void onClickSet(@NonNull final GoogleApiClient client, @NonNull final View button)
  {
    setValue(client, mDataEdit.getText().toString());
  }

  private void setValue(@NonNull final GoogleApiClient client, @NonNull final String value)
  {
    final DataMap dm = new DataMap();
    dm.putString(Const.DATA_KEY_ADHOC, value);
    new UpdateHandler(client).handleUpdate(Const.DATA_KEY_ADHOC, dm);
  }

  public void load(@NonNull final GoogleApiClient client)
  {
    Logger.L("Start loading");
    FeedLoader.startAllLoads(new UpdateHandler(client));
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
