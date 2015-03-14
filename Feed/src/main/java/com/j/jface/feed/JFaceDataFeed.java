package com.j.jface.feed;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
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
  private static final String PEER_ID = "9705384f-ec27-4e68-b880-0dad6c2d9605";
  private static final String DATA_PATH = "/jwatch/Data";

  private final Activity mA;
  private final EditText mDataEdit;
  private final TextView mLog;

  public JFaceDataFeed(final Activity a) {
    mA = a;
    mA.setContentView(R.layout.activity_digital_watch_face_config);
    mDataEdit = (EditText)mA.findViewById(R.id.dataItem);
    mLog = (TextView)mA.findViewById(R.id.log);
  }

  public void Log(final String s) {
    mLog.append(s);
    mLog.append("\n");
  }

  public void onClickSet(final GoogleApiClient client, final View button) {
    setValue(client, mDataEdit.getText().toString());
  }

  private void setValue(final GoogleApiClient client, final String value) {
    PutDataMapRequest putDataMapReq = PutDataMapRequest.create(DATA_PATH);
    putDataMapReq.getDataMap().putString("foobar", value);
    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, putDataReq);
  }

  public void load() {
    Log("clicked");
  }


  // GoogleApiClient.ConnectionCallbacks
  public void onConnected(final GoogleApiClient client, final Bundle connectionHint)
  {
    Uri.Builder builder = new Uri.Builder();
    Uri uri = builder.scheme("wear").path(DATA_PATH).authority(PEER_ID).build();
    Wearable.DataApi.getDataItem(client, uri).setResultCallback(this);
  }

  // ResultCallback
  @Override
  public void onResult(final DataApi.DataItemResult dataItemResult)
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
