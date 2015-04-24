package com.j.jface.feed.actions;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.FutureValue;

public class GetDataAction<T> implements Action, ResultCallback<DataItemBuffer>
{
  private final String mPath;
  private final FutureValue<DataMap> mFuture;

  public GetDataAction(final String path, final FutureValue<DataMap> f)
  {
    mPath = path;
    mFuture = f;
  }

  @Override public void run(@NonNull GoogleApiClient client)
  {
    final Uri uri = new Uri.Builder().scheme("wear")
     .path(mPath)
     .build();
    Wearable.DataApi.getDataItems(client, uri).setResultCallback(this);
  }

  @Override public void onResult(final DataItemBuffer dataItems)
  {
    if (dataItems.getCount() == 1)
      mFuture.set(DataMapItem.fromDataItem(dataItems.get(0)).getDataMap());
    else
      mFuture.set(new DataMap());
    dataItems.release();
  }
}
