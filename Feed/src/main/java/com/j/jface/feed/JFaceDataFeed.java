package com.j.jface.feed;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class JFaceDataFeed implements ResultCallback<DataApi.DataItemResult>
{
  private static final String TAG = "Datafeed";
  static JFaceDataFeed sLogger;

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
    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, putDataReq);
  }

  public void load() {
    FeedLoader.startAllLoads();
  }


  // GoogleApiClient.ConnectionCallbacks
  public void onConnected(@NonNull final GoogleApiClient client, @NonNull final Bundle connectionHint)
  {
    Uri.Builder builder = new Uri.Builder();
    Uri uri = builder.scheme("wear").path(Const.DATA_PATH).authority(Const.PEER_ID).build();
    Wearable.DataApi.getDataItem(client, uri).setResultCallback(this);
  }

  // ResultCallback
  @Override
  public void onResult(@NonNull final DataApi.DataItemResult dataItemResult)
  {
    if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null)
    {
      DataItem configDataItem = dataItemResult.getDataItem();
      DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
      DataMap config = dataMapItem.getDataMap();
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
