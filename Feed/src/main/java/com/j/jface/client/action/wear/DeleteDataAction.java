package com.j.jface.client.action.wear;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;

public class DeleteDataAction extends Action implements ResultCallback<DataApi.DeleteDataItemsResult>
{
  @NonNull private final Uri mUri;

  public DeleteDataAction(@NonNull final Client client, @NonNull final Uri uri)
  {
    super(client, null);
    mUri = uri;
  }

  @Override public void run(@NonNull GoogleApiClient client)
  {
    Wearable.DataApi.deleteDataItems(client, mUri).setResultCallback(this);
  }

  @Override public void onResult(@NonNull final DataApi.DeleteDataItemsResult deleteDataItemsResult)
  {
    finish();
  }
}
