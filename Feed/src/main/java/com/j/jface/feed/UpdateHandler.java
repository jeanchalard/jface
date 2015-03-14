package com.j.jface.feed;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * A class that handles a single update to a piece of data
 */
public class UpdateHandler
{
  @NonNull private final GoogleApiClient mClient;

  public UpdateHandler(@NonNull final GoogleApiClient client) {
    // TODO : instead of taking it as an argument, access a global resource that manages it
    mClient = client;
  }

  public void handleUpdate(@NonNull final PutDataMapRequest data)
  {
    PendingResult<DataApi.DataItemResult> pendingResult =
     Wearable.DataApi.putDataItem(mClient, data.asPutDataRequest());
    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
      @Override
      public void onResult(DataApi.DataItemResult dataItemResult)
      {
        Logger.L(dataItemResult.getDataItem().toString());
      }
    });
  }
}
