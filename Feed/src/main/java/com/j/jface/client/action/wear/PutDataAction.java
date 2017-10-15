package com.j.jface.client.action.wear;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.client.Client;
import com.j.jface.client.action.Action;

import java.util.ArrayList;

public class PutDataAction extends Action implements ResultCallback<DataApi.DataItemResult>
{
  @NonNull private final PutDataRequest mRequest;

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final String key, @NonNull final String value)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putString(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final String key, final boolean value)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putBoolean(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final String key, final long value)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putLong(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final String key, final Asset value)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putAsset(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final String key, final ArrayList<Integer> value)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putIntegerArrayList(key, value);
    mRequest = dmRequest.asPutDataRequest();
  }

  public PutDataAction(@NonNull final Client client, @NonNull final String path, @NonNull final DataMap map)
  {
    super(client, null);
    final PutDataMapRequest dmRequest = PutDataMapRequest.create(path);
    dmRequest.getDataMap().putAll(map);
    mRequest = dmRequest.asPutDataRequest();
  }

  public void run(@NonNull final GoogleApiClient client)
  {
    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(client, mRequest);
    pendingResult.setResultCallback(this);
  }

  @Override public void onResult(@NonNull DataApi.DataItemResult dataItemResult)
  {
    finish();
  }
}
