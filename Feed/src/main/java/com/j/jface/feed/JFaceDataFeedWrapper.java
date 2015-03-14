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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

/**
 * The phone-side config activity for {@code DigitalWatchFaceService}. Like the watch-side config
 * activity ({@code DigitalWatchFaceWearableConfigActivity}), allows for setting the background
 * color. Additionally, enables setting the color for hour, minute and second digits.
 */
public class JFaceDataFeedWrapper extends Activity
 implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
  private static final String TAG = "DigitalWatchFaceConfig";

  @Nullable private JFaceDataFeed mW;
  @Nullable private GoogleApiClient mGoogleApiClient;

  // Activity callbacks
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    mW = new JFaceDataFeed(this);
    mGoogleApiClient = new GoogleApiClient.Builder(this)
     .addConnectionCallbacks(this)
     .addOnConnectionFailedListener(this)
     .addApi(Wearable.API)
     .build();
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    if (null == mGoogleApiClient) return;
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    if (null == mGoogleApiClient) return;
    if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
  }

  // Interface callbacks
  public void onClickSet(@Nullable final View v) {
    if (null == v || null == mW || null == mGoogleApiClient) return;
    mW.onClickSet(mGoogleApiClient, v);
  }

  public void onClickLoad(@Nullable final View v) {
    if (null == mW) return;
    mW.load(mGoogleApiClient);
  }

  // GoogleApiClient.ConnectionCallbacks
  @Override
  public void onConnected(@Nullable final Bundle connectionHint)
  {
    Log.d(TAG, "onConnected : " + connectionHint);
    if (null == connectionHint || null == mW || null == mGoogleApiClient) return;
    mW.onConnected(mGoogleApiClient, connectionHint);
  }

  @Override
  public void onConnectionSuspended(final int cause)
  {
    Log.d(TAG, "onConnectionSuspended : " + cause);
  }

  @Override
  public void onConnectionFailed(final ConnectionResult result)
  {
    Log.d(TAG, "onConnectionFailed : " + result);
  }
}
