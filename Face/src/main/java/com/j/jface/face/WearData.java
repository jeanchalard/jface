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
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.j.jface.Const;

public final class WearData
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
  public static void fetchData(@NonNull final DataClient client,
                               @NonNull final String path,
                               @NonNull final FetchConfigDataMapCallback callback)
  {
    final Uri uri = new Uri.Builder().scheme("wear")
     .path(path)
     .build();
    client.getDataItems(uri).addOnCompleteListener(new DataItemResultCallback(callback));
  }

  /**
   * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
   * If the config DataItem doesn't exist, it's created.
   */
  public static void putConfigDataItem(@NonNull final DataClient client, @NonNull final DataMap newConfig)
  {
    final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Const.CONFIG_PATH);
    final DataMap configToPut = putDataMapRequest.getDataMap();
    putDataMapRequest.setUrgent();
    configToPut.putAll(newConfig);
    client.putDataItem(putDataMapRequest.asPutDataRequest());
  }

  /**
   * General put function to add a String value at in the data path with the key as both the sub path and the key,
   * as is customary for paths that only contain one data item.
   */
  public static void putDataItem(@NonNull final DataClient client, @NonNull final String key, @NonNull final String value)
  {
    final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Const.DATA_PATH + "/" + key);
    final DataMap dm = putDataMapRequest.getDataMap();
    putDataMapRequest.setUrgent();
    dm.putString(key, value);
    client.putDataItem(putDataMapRequest.asPutDataRequest());
  }

  private static class DataItemResultCallback implements OnCompleteListener<DataItemBuffer>
  {
    private final FetchConfigDataMapCallback mCallback;

    private DataItemResultCallback(FetchConfigDataMapCallback callback)
    {
      mCallback = callback;
    }

    @Override public void onComplete(@NonNull final Task<DataItemBuffer> task)
    {
      if (!task.isSuccessful()) return;
      final DataItemBuffer result = task.getResult();
      if (result.getCount() == 1)
      {
        final DataItem item = result.get(0);
        if (result.getStatus().isSuccess() && null != item)
          mCallback.onDataFetched(item.getUri().getPath(), DataMapItem.fromDataItem(item).getDataMap());
      }
      result.release();
    }
  }

  private WearData() {}
}
