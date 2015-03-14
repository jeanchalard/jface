package com.j.jface.feed;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;

public class JFaceDataFeed implements ResultCallback<DataApi.DataItemResult>
{
  @NonNull private final Activity mA;
  @NonNull private final EditText mDataEdit;
  @NonNull private final TextView mLog;

  public JFaceDataFeed(@NonNull final Activity a) {
    mA = a;
    mA.setContentView(R.layout.activity_digital_watch_face_config);
    mDataEdit = (EditText)mA.findViewById(R.id.dataItem);
    mLog = (TextView)mA.findViewById(R.id.log);
    Logger.setLogger(this);
  }

  public void Log(@NonNull final String s) {
    mA.runOnUiThread(new Runnable() { public void run()
    {
      mLog.append(s);
      mLog.append("\n");
    }});
  }

  public void onClickSet(@NonNull final GoogleApiClient client, @NonNull final View button) {
    setValue(client, mDataEdit.getText().toString());
  }

  private void setValue(@NonNull final GoogleApiClient client, @NonNull final String value) {
    PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Const.DATA_PATH);
    putDataMapReq.getDataMap().putString("foobar", value);
    new UpdateHandler(client).handleUpdate(putDataMapReq);
  }

  public void load(@NonNull final GoogleApiClient client) {
    FeedLoader.startAllLoads(new UpdateHandler(client));
  }


  // GoogleApiClient.ConnectionCallbacks
  public void onConnected(@Nullable final GoogleApiClient client, @NonNull final Bundle connectionHint)
  {
    Uri.Builder builder = new Uri.Builder();
    Uri uri = builder.scheme("wear").path(Const.DATA_PATH).authority(Const.PEER_ID).build();
    Wearable.DataApi.getDataItem(client, uri).setResultCallback(this);
  }

  // ResultCallback
  @Override
  public void onResult(@NonNull final DataApi.DataItemResult dataItemResult)
  {
    Log.e("\033[3mRESULT\033[0m", dataItemResult.toString());
    if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null)
    {
      DataItem configDataItem = dataItemResult.getDataItem();
      DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
      DataMap config = dataMapItem.getDataMap();
      Log.e("HERE", config.toString());
    }
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
