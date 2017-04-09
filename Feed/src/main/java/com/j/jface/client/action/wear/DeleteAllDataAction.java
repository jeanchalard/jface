package com.j.jface.client.action.wear;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.client.action.Action;
import com.j.jface.client.Client;

public class DeleteAllDataAction extends Action implements ResultCallback<DataItemBuffer>
{
  public DeleteAllDataAction(@NonNull final Client client)
  {
    super(client, null);
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
    // Because it initializes its dependency with null explicitly, it does not matter if this object calls finish() or not.
    // If someone ever cares about it, then it should be implemented correctly, waiting for all spawned data deletions to
    // end and then calling finish() appropriately.
  }
}
