package com.j.jface.feed.actions;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;
import com.j.jface.feed.Logger;

public class PutDataAction implements Action, ResultCallback<DataApi.DataItemResult>
{
  @NonNull final PutDataRequest mRequest;

  public PutDataAction(@NonNull final String dataName, @NonNull final String key, @NonNull final String value)
  {
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(Const.DATA_PATH + "/" + dataName);
    dmRequest.getDataMap().putString(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final String dataName, @NonNull final String key, final boolean value)
  {
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(Const.DATA_PATH + "/" + dataName);
    dmRequest.getDataMap().putBoolean(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final String dataName, @NonNull final DataMap map)
  {
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(Const.DATA_PATH + "/" + dataName);
    dmRequest.getDataMap().putAll(map);
    mRequest = dmRequest.asPutDataRequest();
  }

  public void run(@NonNull final GoogleApiClient client)
  {
    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, mRequest);
    pendingResult.setResultCallback(this);
  }

  @Override
  public void onResult(DataApi.DataItemResult dataItemResult)
  {
    final DataMap dm = DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap();
    for (final String k : dm.keySet())
      Logger.L(k + "=" + dm.get(k).toString());
  }
}
