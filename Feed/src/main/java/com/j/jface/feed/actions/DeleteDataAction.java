package com.j.jface.feed.actions;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

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
    Log.e("Jface", "Deleted data for " + mUri);
  }
}
