package com.j.jface.client.action.wear;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.FutureValue;
import com.j.jface.client.action.Action;
import com.j.jface.client.Client;
import com.j.jface.client.action.ResultAction;

public class GetDataAction extends ResultAction<DataMap> implements ResultCallback<DataItemBuffer>
{
  @NonNull private final String mPath;
  @Nullable private final Client.GetDataCallback mCallback;

  public GetDataAction(@NonNull final Client client, @Nullable Action then, @NonNull final String path)
  {
    super(client, then);
    mPath = path;
    mCallback = null;
  }

  public GetDataAction(@NonNull final Client client, @Nullable Action then, @NonNull final String path, @NonNull final Client.GetDataCallback callback)
  {
    super(client, then);
    mPath = path;
    mCallback = callback;
  }

  @Override public void run(@NonNull GoogleApiClient client)
  {
    final Uri uri = new Uri.Builder().scheme("wear")
     .path(mPath)
     .build();
    Wearable.DataApi.getDataItems(client, uri).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final DataItemBuffer dataItems)
  {
    final DataMap result;
    if (dataItems.getCount() == 1)
    {
      final DataMap dataMap = DataMapItem.fromDataItem(dataItems.get(0)).getDataMap();
      if (null != mCallback) mCallback.run(mPath, dataMap);
      result = dataMap;
    }
    else
    {
      if (null != mCallback) mCallback.run(mPath, new DataMap());
      result = new DataMap();
    }
    dataItems.release();
    finish(result);
  }
}
