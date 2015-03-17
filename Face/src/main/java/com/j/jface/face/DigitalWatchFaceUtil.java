/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.face;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.j.jface.Const;

public final class DigitalWatchFaceUtil
{
  /**
   * Callback interface to perform an action with the current config {@link DataMap} for
   * {@link DigitalWatchFaceService}.
   */
  public interface FetchConfigDataMapCallback
  {
    /**
     * Callback invoked with the current data for {@link DigitalWatchFaceService}.
     */
    void onDataFetched(@NonNull final String path, @NonNull final DataMap data);
  }

  /**
   * Asynchronously fetches the current config {@link DataMap} for {@link DigitalWatchFaceService}
   * and passes it to the given callback.
   * <p>
   * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
   * receives an empty DataMap.
   */
  public static void fetchData(final GoogleApiClient client,
                               final String path,
                               final FetchConfigDataMapCallback callback)
  {
    final Uri uri = new Uri.Builder().scheme("wear")
     .path(path)
     .build();
    Wearable.DataApi.getDataItems(client, uri).setResultCallback(new DataItemResultCallback(callback));
  }

  /**
   * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with
   * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
   * it's created.
   * <p>
   * It is allowed that only some of the keys used in the config DataItem appear in
   * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
   */
  public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
                                                  final DataMap configKeysToOverwrite)
  {

    DigitalWatchFaceUtil.fetchData(googleApiClient, Const.CONFIG_PATH,
     new FetchConfigDataMapCallback()
     {
       @Override
       public void onDataFetched(@NonNull final String path, @NonNull final DataMap currentConfig)
       {
         DataMap overwrittenConfig = new DataMap();
         overwrittenConfig.putAll(currentConfig);
         overwrittenConfig.putAll(configKeysToOverwrite);
         DigitalWatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
       }
     }
    );
  }

  /**
   * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
   * If the config DataItem doesn't exist, it's created.
   */
  public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig)
  {
    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Const.CONFIG_PATH);
    DataMap configToPut = putDataMapRequest.getDataMap();
    configToPut.putAll(newConfig);
    Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest());
  }

  private static class DataItemResultCallback implements ResultCallback<DataItemBuffer>
  {
    private final FetchConfigDataMapCallback mCallback;

    public DataItemResultCallback(FetchConfigDataMapCallback callback)
    {
      mCallback = callback;
    }

    @Override
    public void onResult(@NonNull DataItemBuffer result)
    {
      if (result.getCount() == 1)
      {
        final DataItem item = result.get(0);
        if (result.getStatus().isSuccess() && null != item)
          mCallback.onDataFetched(item.getUri().getPath(), DataMapItem.fromDataItem(item).getDataMap());
      }
      result.release();
    }
  }

  private DigitalWatchFaceUtil() {}
}
