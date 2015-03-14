/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.feed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
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

/**
 * The phone-side config activity for {@code DigitalWatchFaceService}. Like the watch-side config
 * activity ({@code DigitalWatchFaceWearableConfigActivity}), allows for setting the background
 * color. Additionally, enables setting the color for hour, minute and second digits.
 */
public class JFaceDataFeed extends Activity
 implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
 ResultCallback<DataApi.DataItemResult>
{
  private static final String TAG = "DigitalWatchFaceConfig";

  // TODO: use the shared constants (needs covering all the samples with Gradle build model)
  private static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";
  private static final String KEY_HOURS_COLOR = "HOURS_COLOR";
  private static final String KEY_MINUTES_COLOR = "MINUTES_COLOR";
  private static final String KEY_SECONDS_COLOR = "SECONDS_COLOR";
  private static final String DATA_PATH = "/jwatch/Data";

  private GoogleApiClient mGoogleApiClient;
  private final String mPeerId = "9705384f-ec27-4e68-b880-0dad6c2d9605";
  private EditText mDataEdit;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_digital_watch_face_config);

    mGoogleApiClient = new GoogleApiClient.Builder(this)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API)
     .build();

    ComponentName name = getIntent().getParcelableExtra(WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);
    mDataEdit = (EditText) findViewById(R.id.dataItem);
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop()
  {
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
    {
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override // GoogleApiClient.ConnectionCallbacks
  public void onConnected(Bundle connectionHint)
  {
    Log.d(TAG, "onConnected: " + connectionHint);

    if (mPeerId != null)
    {
      Uri.Builder builder = new Uri.Builder();
      Uri uri = builder.scheme("wear").path(DATA_PATH).authority(mPeerId).build();
      Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
    }
    else
    {
      displayNoConnectedDeviceDialog();
    }
  }

  public void onClickSet(final View v) {
    Log.e("CLICK", "set : " + mDataEdit.getText().toString());
    setValue(mDataEdit.getText().toString());
  }

  private void setValue(final String value) {
    PutDataMapRequest putDataMapReq = PutDataMapRequest.create(DATA_PATH);
    putDataMapReq.getDataMap().putString("foobar", value);
    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
    PendingResult<DataApi.DataItemResult> pendingResult =
     Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
  }

  @Override // ResultCallback<DataApi.DataItemResult>
  public void onResult(DataApi.DataItemResult dataItemResult)
  {
    if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null)
    {
      DataItem configDataItem = dataItemResult.getDataItem();
      DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
      DataMap config = dataMapItem.getDataMap();
    }
  }

  @Override // GoogleApiClient.ConnectionCallbacks
  public void onConnectionSuspended(int cause)
  {
    if (Log.isLoggable(TAG, Log.DEBUG))
    {
      Log.d(TAG, "onConnectionSuspended: " + cause);
    }
  }

  @Override // GoogleApiClient.OnConnectionFailedListener
  public void onConnectionFailed(ConnectionResult result)
  {
    if (Log.isLoggable(TAG, Log.DEBUG))
    {
      Log.d(TAG, "onConnectionFailed: " + result);
    }
  }

  private void displayNoConnectedDeviceDialog()
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    String messageText = getResources().getString(R.string.title_no_device_connected);
    String okText = getResources().getString(R.string.ok_no_device_connected);
    builder.setMessage(messageText)
     .setCancelable(false)
     .setPositiveButton(okText, new DialogInterface.OnClickListener()
     {
       public void onClick(DialogInterface dialog, int id)
       {
       }
     });
    AlertDialog alert = builder.create();
    alert.show();
  }

  private void sendConfigUpdateMessage(String configKey, String msg)
  {
    if (mPeerId != null)
    {
      DataMap config = new DataMap();
      config.putString(configKey, msg);
      byte[] rawData = config.toByteArray();
      Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, DATA_PATH, rawData);
    }
  }
}
