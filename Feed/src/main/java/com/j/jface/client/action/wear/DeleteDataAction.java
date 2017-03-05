package com.j.jface.client.action.wear;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.client.action.Action;

public class DeleteDataAction implements Action
{
  @NonNull private final Uri mUri;

  public DeleteDataAction(@NonNull final Uri uri)
  {
    mUri = uri;
  }

  @Override public void run(@NonNull GoogleApiClient client)
  {
    Wearable.DataApi.deleteDataItems(client, mUri);
  }
}
