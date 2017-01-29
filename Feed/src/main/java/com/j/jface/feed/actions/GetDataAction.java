package com.j.jface.feed.actions;

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
import com.j.jface.feed.Client;

public class GetDataAction implements Action, ResultCallback<DataItemBuffer>
{
  @NonNull private final String mPath;
  @Nullable private final FutureValue<DataMap> mFuture;
  @Nullable private final Client.GetDataCallback mCallback;

  public GetDataAction(@NonNull final String path, @NonNull final FutureValue<DataMap> f)
  {
    mPath = path;
    mFuture = f;
    mCallback = null;
  }

  public GetDataAction(@NonNull final String path, @NonNull final Client.GetDataCallback callback)
  {
    mPath = path;
    mFuture = null;
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
    if (dataItems.getCount() == 1)
    {
      final DataMap dataMap = DataMapItem.fromDataItem(dataItems.get(0)).getDataMap();
      if (null != mCallback) mCallback.run(mPath, dataMap);
      if (null != mFuture) mFuture.set(dataMap);
    }
    else
    {
      if (null != mCallback) mCallback.run(mPath, new DataMap());
      if (null != mFuture) mFuture.set(new DataMap());
    }
    dataItems.release();
  }
}
