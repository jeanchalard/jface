package com.j.jface.client.action.wear;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.client.action.Action;
import com.j.jface.client.Client;

public class DeleteAllDataAction implements Action, ResultCallback<DataItemBuffer>
{
  @NonNull private final Client mClient;

  public DeleteAllDataAction(@NonNull final Client client)
  {
    mClient = client;
  }

  @Override public void run(@NonNull final GoogleApiClient client)
  {
    Wearable.DataApi.getDataItems(client).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final DataItemBuffer dataItems)
  {
    final int count = dataItems.getCount();
    for (int i = 0; i < count; ++i)
      mClient.deleteData(dataItems.get(i).getUri());
  }
}
